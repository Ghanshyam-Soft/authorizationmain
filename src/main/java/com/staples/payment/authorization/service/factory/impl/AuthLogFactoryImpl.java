package com.staples.payment.authorization.service.factory.impl;

import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.details.AuthReqAddressInfo;
import com.staples.payment.authorization.request.details.AuthReqCOFInfo;
import com.staples.payment.authorization.request.details.AuthReqCardInfo;
import com.staples.payment.authorization.request.details.AuthReqTransactionDetail;
import com.staples.payment.authorization.request.details.AuthReqTransactionHeader;
import com.staples.payment.authorization.service.factory.AuthLogFactory;
import com.staples.payment.shared.cache.BusinessMasterCache;
import com.staples.payment.shared.constant.MessageStatus;
import com.staples.payment.shared.constant.PaymentMethod;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.AuthLog.AuthLogBuilder;
import com.staples.payment.shared.entity.BusinessMaster;
import com.staples.payment.shared.entity.MerchantMaster;
import com.staples.payment.shared.util.GuidUtil;

import lombok.val;

@Component
public class AuthLogFactoryImpl implements AuthLogFactory
{
	private final BusinessMasterCache businessMasterCache;
	private final GuidUtil guidUtil;

	public AuthLogFactoryImpl(GuidUtil guidUtil, BusinessMasterCache businessMasterCache)
	{
		this.businessMasterCache = businessMasterCache;
		this.guidUtil = guidUtil;
	}

	@Override
	public AuthLog createAuthLog(final AuthRequest request, Instant receivedTime)
	{
		val transactionHeader = request.getTransactionHeader();
		val cardInfo = request.getCardInfo();

		MerchantMaster merchantMaster = retrieveMerchantMaster(transactionHeader.getBusinessUnit(), transactionHeader.getDivision(), cardInfo.getPaymentType());

		final String gpasKey = createGpasKey(merchantMaster);

		AuthLog authLog = convertToAuthLog(request, false, receivedTime, gpasKey, merchantMaster);
		return authLog;
	}

	private MerchantMaster retrieveMerchantMaster(String businessUnit, String division, PaymentMethod paymentType)
	{
		// TODO: Fully switch to the new caching mechanism

		/*		// TODO: Currently the caching isn't storing by businessUnit/division, need to fix that. Could just switch to using businessUnit and division as ids instead of the artificial id.
				BusinessMaster businessMaster = businessMasterRepo.findByBusinessUnitAndBusinessDivision(transactionHeader.getBusinessUnit(), transactionHeader.getDivision());
		
				for(MerchantMaster merchantMaster : businessMaster.getMerchantMasterList())
				{
					if(merchantMaster.getPaymentMethod().equals(cardInfo.getPaymentType()))
					{
						return merchantMaster;
					}
				}*/

		return businessMasterCache.getMerchantMasterBy(businessUnit, division, paymentType);

		// throw new InvalidInputException("No matching MerchantMaster");
	}

	/*
	 * Creates a GpasKey. The requirements were that it needs to be unique without special characters and meet bank requirements.
	 * 
	 * I ended up using the same logic as how we generate the settleGuid for simplicity.
	 */
	private String createGpasKey(MerchantMaster merchantMaster)
	{
		final BusinessMaster businessMaster = merchantMaster.getBusinessMaster();

		final String gpasKey = guidUtil.createGuid(businessMaster);
		return gpasKey;
	}

	private AuthLog convertToAuthLog(AuthRequest request, boolean isDuplicate, Instant receivedTime, String gpasKey, MerchantMaster merchantMaster)
			throws InvalidInputException
	{
		final AuthReqTransactionHeader header = request.getTransactionHeader();
		final AuthReqTransactionDetail detail = request.getTransactionDetail();
		final AuthReqCardInfo cardInfo = request.getCardInfo();
		final AuthReqCOFInfo cofInfo = request.getCofInfo();
		final AuthReqAddressInfo addressInfo = request.getAddressInfo();

		AuthLogBuilder authLogBuilder = AuthLog.builder()
				.childKey(header.getChildGUID())
				.gpasKey(gpasKey)
				.paymentType(header.getPaymentType())
				.requestType(header.getRequestType())
				.bank(merchantMaster.getBankName())
				.txnDatetime(header.getUtcDateTime())
				.txnLocalDatetime(header.getLocalDateTime())
				.countryCode(header.getCountry())
				.businessUnit(header.getBusinessUnit())
				.businessDivision(header.getDivision())
				.parentKey(header.getParentGUID())
				.authReferenceKey(header.getAuthReferenceGUID()) // Gift-card Integration
				.originatingKey(header.getOriginatingGUID())
				.reversalKey(header.getReversalGUID())
				.orderNo(header.getOrderNumber())
				.storeNo(header.getStoreNumber())
				.storeRegisterNo(header.getStoreRegNumber())
				.storeTransactionNo(header.getStoreTransNumber())
				.paymentToken(detail.getPaymentToken())
				// .cardBin(CARD_BIN)
				.giftCardNumber(detail.getGiftCardNumber()) // Gift-card Integration
				.cardExpiry(cardInfo.getExpirationDate())
				.paymentMethod(cardInfo.getPaymentType())
				.posDataCode(detail.getPosDataCode().toCodeString())
				.saleAmount(detail.getSaleAmount())
				.transactionAmount(detail.getTransactionAmount())
				.clientRefKey(header.getClientReferenceKey());
		// .cashbackAmount(detail.getCashbackAmount())

		if(!isDuplicate)// TODO: isDuplicate is likely extraneous and could just use merchantMaster != null
		{
			final BusinessMaster businessMaster = merchantMaster.getBusinessMaster();

			final String storeNo = (businessMaster.getGpasStoreNo() == null) ? null : businessMaster.getGpasStoreNo().toString();// TODO: Should gpasStoreNo just be a String anyways?

			authLogBuilder = authLogBuilder
					.merchantMaster(merchantMaster)
					.merchantUniqueId(businessMaster.getMerchantUniqueId())
					// .merchantBankUniqueId(merchantMaster.getMerchantBankUniqueId())
					.storeNo(storeNo)
					.vendorInfoBlock(merchantMaster.getBankProviderInfoBlock());
		}

		if(cofInfo != null)
		{
			authLogBuilder = authLogBuilder
					.cofTransactionInitiator(cofInfo.getTransactionInitiator())
					.cofStoredCredInd(cofInfo.getStoredCredInd())
					.cofSchedInd(cofInfo.getCofSchedInd())
					.cofBillingType(cofInfo.getBillingType());
		}

		if(addressInfo != null)
		{
			authLogBuilder = authLogBuilder
					.ipAddress(addressInfo.getIpAddress());
		}

		authLogBuilder = authLogBuilder
				.requestReceiveDatetime(receivedTime)
				.requestSentToBankDatetime(Instant.now());

		if(StringUtils.hasText(request.getCardInfo().getXid()))
		{
			authLogBuilder.threeDsFlag(true);
		}
		return authLogBuilder.build();
	}

	@Override
	public AuthLog createDuplicateAuthLog(final AuthRequest request, Instant receivedTime, AuthLog existingAuthLog) throws InvalidInputException
	{
		// For duplicate the auth log should have transaction header columns from the new request, transaction detail and card info columns from the existing auth log.
		// Hence the auth log entity is created using the response

		final String gpasKey = existingAuthLog.getGpasKey();

		AuthLog newAuthLog = convertToAuthLog(request, true, receivedTime, gpasKey, existingAuthLog.getMerchantMaster());

		newAuthLog.setResponseSentDatetime(Instant.now());
		newAuthLog.setMessageStatus(MessageStatus.Duplicate);

		// message status will not be copied ..it will be set from calling process
		// response sent datetime will be populated from calling process and not be copied
		// newAuthLog.setMerchantBankUniqueId(existingAuthLog.getMerchantBankUniqueId());

		newAuthLog.setMerchantMaster(existingAuthLog.getMerchantMaster());
		newAuthLog.setMerchantUniqueId(existingAuthLog.getMerchantUniqueId());
		newAuthLog.setApprovedAmount(existingAuthLog.getApprovedAmount());
		newAuthLog.setAuthCode(existingAuthLog.getAuthCode());
		newAuthLog.setRemainingBalanceAmount(existingAuthLog.getRemainingBalanceAmount());

		newAuthLog.setGpasRespCode(existingAuthLog.getGpasRespCode());
		newAuthLog.setGpasReasCode(existingAuthLog.getGpasReasCode());
		newAuthLog.setGpasAvsCode(existingAuthLog.getGpasAvsCode());
		newAuthLog.setGpasCvvCode(existingAuthLog.getGpasCvvCode());
		newAuthLog.setGpasResponseDescription(existingAuthLog.getGpasResponseDescription());
		newAuthLog.setVendorRespCode(existingAuthLog.getVendorRespCode());
		newAuthLog.setVendorReasCode(existingAuthLog.getVendorReasCode());
		newAuthLog.setVendorAvsCode(existingAuthLog.getVendorAvsCode());
		newAuthLog.setVendorCvvCode(existingAuthLog.getVendorCvvCode());
		newAuthLog.setVendorInfoBlock(existingAuthLog.getVendorInfoBlock());

		newAuthLog.setRespRcvdFromBankDatetime(existingAuthLog.getRespRcvdFromBankDatetime());
		newAuthLog.setPaymentToken(existingAuthLog.getPaymentToken());
		newAuthLog.setPaymentMethod(existingAuthLog.getPaymentMethod());

		newAuthLog.setDefaultResponse(existingAuthLog.isDefaultResponse());

		// Setting responses here doesn't effect db, rather it ensures that the object in memory matches the db
		newAuthLog.setAciResponse(existingAuthLog.getAciResponse());
		newAuthLog.setBraintreeResponse(existingAuthLog.getBraintreeResponse());
		newAuthLog.setBamboraResponse(existingAuthLog.getBamboraResponse());
		newAuthLog.setAmexRedeemResponse(existingAuthLog.getAmexRedeemResponse());
		newAuthLog.setCybersourceResponse(existingAuthLog.getCybersourceResponse());
		newAuthLog.setThreeDSResponse(existingAuthLog.getThreeDSResponse());

		return newAuthLog;
	}
}