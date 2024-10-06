package com.staples.payment.authorization.service.factory.impl;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.details.AuthReqTransactionHeader;
import com.staples.payment.authorization.response.AuthResponse;
import com.staples.payment.authorization.response.details.AuthRespAmexPWP;
import com.staples.payment.authorization.response.details.AuthRespBalanceInfo;
import com.staples.payment.authorization.response.details.AuthRespCOFInfo;
import com.staples.payment.authorization.response.details.AuthRespPWPReservationInfo;
import com.staples.payment.authorization.response.details.AuthRespPayPalInfo;
import com.staples.payment.authorization.response.details.AuthRespTransactionDetail;
import com.staples.payment.authorization.response.details.AuthRespTransactionHeader;
import com.staples.payment.authorization.service.factory.ResponseFactory;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.Bank;
import com.staples.payment.shared.constant.GpasRespCode;
import com.staples.payment.shared.constant.PaymentMethod;
import com.staples.payment.shared.constant.PaymentType;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.MerchantMaster;
import com.staples.payment.shared.entity.ThreeDSResponse;
import com.staples.payment.shared.entity.aci.AciAuthResponse;
import com.staples.payment.shared.entity.bank.AmexRedeemResponse;
import com.staples.payment.shared.entity.braintree.BraintreeResponse;

import lombok.val;

@Component
public class ResponseFactoryImpl implements ResponseFactory
{
	private final ObjectMapper objectMapper;

	public ResponseFactoryImpl(ObjectMapper objectMapper)
	{
		this.objectMapper = objectMapper;
	}

	@Override
	public AuthResponse createAuthResponse(AuthLog authLog, AuthRequest request)
	{
		final AuthReqTransactionHeader requestTransactionHeader = request.getTransactionHeader();

		AuthRespAmexPWP amexPwpResp = null;
		if(authLog.getAmexRedeemResponse() != null)
		{
			amexPwpResp = createPayPointsAuthResponse(authLog.getAmexRedeemResponse());
		}

		AuthRespTransactionHeader transactionHeader = getResponseTranHeaderFrom(requestTransactionHeader);

		AuthRespPayPalInfo paypalInfo = null;
		Instant authorizationExpiresAt = null;
		if(requestTransactionHeader.getPaymentType() == PaymentType.PayPal) // TODO: See if can reduce number of times we find braintree response from repo
		{
			boolean transactionApproved = (authLog.getGpasRespCode() == GpasRespCode.A);

			BraintreeResponse braintreeResponse = authLog.getBraintreeResponse();
			if(braintreeResponse != null)
			{
				paypalInfo = createPaypalResponseInfo(braintreeResponse, transactionApproved);
				authorizationExpiresAt = getPayPalAuthorizationExpiration(requestTransactionHeader, transactionApproved, braintreeResponse);
			}
			else if(transactionApproved && !authLog.isDefaultResponse())
			{
				throw new RuntimeException("AuthLog for Braintree transaction is Approved and not a default response but Braintree response is missing.");
			}
		}

		AuthRespCOFInfo cofInfo = null;
		AciAuthResponse aciResponse = null;
		if(authLog.getBank() == Bank.ACI)
		{
			aciResponse = authLog.getAciResponse();
			cofInfo = createCofResponseInfo(authLog, aciResponse);
		}

		AuthRespTransactionDetail transactionDetail = getResponseTranDetailFromAuthLog(authLog, authorizationExpiresAt);

		if(aciResponse != null)
		{
			transactionDetail.setCavvResultCode(aciResponse.getCavvResultCode());
		}

		ThreeDSResponse threeDSResponse = authLog.getThreeDSResponse();
		if(threeDSResponse != null)
		{
			// populate threeDSDTO values in the auth-response
			transactionDetail.setCavv(threeDSResponse.getEncodedCReq());
			transactionDetail.setXid(threeDSResponse.getThreeDSServerTransID());
			transactionDetail.setEciFlag(threeDSResponse.getEci());
		}

		AuthResponse response = AuthResponse.builder()
				.transactionDetail(transactionDetail)
				.transactionHeader(transactionHeader)
				.payPalInfo(paypalInfo)
				.amexPWP(amexPwpResp)
				.cofInfo(cofInfo)
				.build();

		return response;
	}

	private @Nullable AuthRespCOFInfo createCofResponseInfo(AuthLog authLog, AciAuthResponse aciResponse)
	{
		val paymentMethod = authLog.getPaymentMethod();

		if(aciResponse == null)
		{
			if(authLog.getGpasRespCode() == GpasRespCode.A && !authLog.isDefaultResponse())
			{
				throw new RuntimeException("AuthLog for ACI transaction is Approved and not a default response but ACI response is missing.");
			}

			return null;
		}

		String initialTransactionId = null;
		if(paymentMethod == PaymentMethod.VI || paymentMethod == PaymentMethod.DI || paymentMethod == PaymentMethod.AM)// In future will need to take into accounts changing payment method based on ACI response.
		{
			initialTransactionId = aciResponse.getBankTransactionId();
		}
		else if(paymentMethod == PaymentMethod.MC)
		{
			initialTransactionId = aciResponse.getBanknetRefNr();
		}

		if(initialTransactionId == null && aciResponse.getBanknetDate() == null) // per Pratima "if all contained fields are null, return null"
		{
			return null;
		}

		return AuthRespCOFInfo.builder()
				.initialTransactionId(initialTransactionId)
				.initialTransactionDate(aciResponse.getBanknetDate())
				.build();
	}

	private AuthRespTransactionHeader getResponseTranHeaderFrom(AuthReqTransactionHeader reqTranHeader)
	{
		AuthRespTransactionHeader respTranHeader = objectMapper.convertValue(reqTranHeader, AuthRespTransactionHeader.class);
		return respTranHeader;
	}

	private AuthRespTransactionDetail getResponseTranDetailFromAuthLog(AuthLog authLog, @Nullable Instant authorizationExpiresAt)
	{
		MerchantMaster merchantMaster = authLog.getMerchantMaster();

		// TODO: I believe we need to get this from the response
		PaymentMethod methodProcessed = (merchantMaster != null) ? merchantMaster.getPaymentMethod() : null; // TODO: Should we throw an error in the case merchantMaster doesn't exist? Currently this is listed as a required field.

		AuthRespTransactionDetail respTranDtl = AuthRespTransactionDetail.builder()
				.amountApproved(authLog.getApprovedAmount() == null ? new BigDecimal("0.00") : authLog.getApprovedAmount())
				.amountRequested(authLog.getTransactionAmount())
				.authorizationCode(authLog.getAuthCode())
				.avsResponseCode(authLog.getGpasAvsCode())
				.ccinResponseCode(authLog.getGpasCvvCode())
				.descriptionText(authLog.getGpasResponseDescription())
				.messageStatus(authLog.getMessageStatus())
				.methodProcessed(methodProcessed)
				.poRequiredResponseCode(false)
				.reasonCode(authLog.getGpasReasCode())
				.remainingBalance(authLog.getRemainingBalanceAmount() == null ? new BigDecimal("0.00") : authLog.getRemainingBalanceAmount())
				.responseCode(authLog.getGpasRespCode())
				.vendorAVSResponseCode(authLog.getVendorAvsCode())
				.vendorCCINResponseCode(authLog.getVendorCvvCode())
				.vendorInfoBlock(authLog.getVendorInfoBlock())
				.vendorReasonCode(authLog.getVendorReasCode())
				.vendorResponseCode(authLog.getVendorRespCode())
				.authorizationExpiresAt(authorizationExpiresAt)
				.paymentToken(authLog.getPaymentToken())
				.build();

		return respTranDtl;
	}

	private AuthRespPayPalInfo createPaypalResponseInfo(BraintreeResponse braintreeEntity, boolean transactionApproved)
	{
		if(transactionApproved && braintreeEntity.getTransactionId() == null)
		{
			throw new RuntimeException("AuthLog for Braintree transaction is Approved but Braintree TransactionId is null.");
		}

		return AuthRespPayPalInfo.builder()
				.deviceData(braintreeEntity.getDeviceData())
				.payerEmail(braintreeEntity.getPayerEmail())
				.payerId(braintreeEntity.getPayerId())
				.payerPhone(braintreeEntity.getPayerPhone())
				.payerStatus(braintreeEntity.getPayerStatus())
				.sellerProtectionStatus(braintreeEntity.getSellerProtectionStatus())
				.transactionId(braintreeEntity.getTransactionId())
				.build();
	}

	private Instant getPayPalAuthorizationExpiration(final AuthReqTransactionHeader requestTransactionHeader, boolean transactionApproved, BraintreeResponse braintreeResponse)
	{
		Instant authorizationExpiresAt = braintreeResponse.getAuthorizationExpiresAt();
		if(transactionApproved && requestTransactionHeader.getRequestType() == AuthRequestType.Lookup && authorizationExpiresAt == null)
		{
			throw new RuntimeException("AuthLog for Looked up Braintree transaction is Approved but authorizationExpiresAt is null.");
		}
		return authorizationExpiresAt;
	}

	private AuthRespAmexPWP createPayPointsAuthResponse(AmexRedeemResponse response)
	{
		val pwpBalanceInfo = AuthRespBalanceInfo.builder()
				.balanceRewardsUnit("POINTS")
				.balanceRewardsAmount(response.getAmount())
				.balanceCurrencyAmount(response.getCurrentBalance())
				.balanceCurrencyCode(response.getBasketAmountCurrencyCode())
				.build();

		val reservationInfo = AuthRespPWPReservationInfo.builder()
				.currencyAmount(response.getBasketAmount())
				.currencyCode(response.getAmountCurrencyCode())
				.rewardsUnit("POINTS")
				.rewardsAmount(response.getBasketAmount())
				.build();

		return AuthRespAmexPWP.builder()
				.pwpBalanceInfo(pwpBalanceInfo)
				.pwpReservationInfo(reservationInfo)
				.pwpResponseCode(response.getResponseCode())
				.pwpResponseDesc(response.getDescriptionText())
				.build();
	}
}