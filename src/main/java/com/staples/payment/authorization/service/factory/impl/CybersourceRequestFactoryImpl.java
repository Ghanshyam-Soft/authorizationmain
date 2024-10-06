package com.staples.payment.authorization.service.factory.impl;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.details.AuthReqAddressInfo;
import com.staples.payment.authorization.request.details.AuthReqCardInfo;
import com.staples.payment.authorization.request.details.AuthReqTransactionDetail;
import com.staples.payment.authorization.service.factory.CybersourceRequestFactory;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.Currency;
import com.staples.payment.shared.cybersource.constants.CSHeaders;
import com.staples.payment.shared.cybersource.dto.common.Card;
import com.staples.payment.shared.cybersource.dto.request.CSPaymentsRequest;
import com.staples.payment.shared.cybersource.dto.request.CSReversalRequest;
import com.staples.payment.shared.cybersource.dto.request.common.AmountDetails;
import com.staples.payment.shared.cybersource.dto.request.common.BillTo;
import com.staples.payment.shared.cybersource.dto.request.common.ClientReferenceInformation;
import com.staples.payment.shared.cybersource.dto.request.common.OrderInformation;
import com.staples.payment.shared.cybersource.dto.request.common.PaymentInformation;
import com.staples.payment.shared.cybersource.dto.request.reversal.ReversalInformation;
import com.staples.payment.shared.entity.MerchantMaster;
import com.staples.payment.shared.entity.cybersource.CybersourceAuthResponse;

import lombok.val;

@Service
public class CybersourceRequestFactoryImpl implements CybersourceRequestFactory
{
	@Override
	public Map<String, String> createHeaders(String gpasKey, MerchantMaster merchantMaster)
	{
		Map<String, String> headersMap = new HashMap<>();

		headersMap.put(CSHeaders.UNIQUE_ID, gpasKey);
		headersMap.put(CSHeaders.MERCHANT_ID, merchantMaster.getBankMerchantId());
		headersMap.put(CSHeaders.MERCHANT_KEY_ID, merchantMaster.getMerchantKeyId());
		headersMap.put(CSHeaders.MERCHANT_SHARED_KEY, merchantMaster.getMerchantSharedKey());

		return headersMap;
	}

	@Override
	public CSPaymentsRequest createPaymentRequest(AuthRequest gpasRequest)
	{
		AuthRequestType requestType = gpasRequest.getTransactionHeader().getRequestType();
		val transactionDetail = gpasRequest.getTransactionDetail();
		val cardInfo = gpasRequest.getCardInfo();
		val addressInfo = gpasRequest.getAddressInfo();

		final PaymentInformation paymentInformation = createPaymentInfo(transactionDetail.getPaymentToken(), cardInfo);
		final OrderInformation orderInformation = addOrderInfo(requestType, transactionDetail, addressInfo);

		CSPaymentsRequest request = CSPaymentsRequest.builder()
				.paymentInformation(paymentInformation)
				.orderInformation(orderInformation)
				.build();

		return request;
	}

	private PaymentInformation createPaymentInfo(String paymentToken, AuthReqCardInfo cardInfo)
	{
		Card card = addCardDetails(paymentToken, cardInfo);

		return PaymentInformation.builder()
				.card(card)
				.build();
	}

	private Card addCardDetails(String paymentToken, AuthReqCardInfo cardInfo)
	{
		String expiryDate = convertExpDate(cardInfo.getExpirationDate());

		return Card.builder()
				.number(paymentToken)
				.expirationMonth(expiryDate.substring(0, 2))
				.expirationYear(expiryDate.substring(2, 4))
				.build();
	}

	private OrderInformation addOrderInfo(AuthRequestType requestType, AuthReqTransactionDetail transactionDetail, AuthReqAddressInfo addressInfo)
	{
		final AmountDetails amountDetails = addAmountDetails(requestType, transactionDetail);
		final BillTo billTo = createBillingAddress(addressInfo);

		return OrderInformation.builder()
				.amountDetails(amountDetails)
				.billTo(billTo)
				.build();
	}

	private AmountDetails addAmountDetails(AuthRequestType requestType, AuthReqTransactionDetail transactionDetail)
	{
		final BigDecimal totalAmount;
		if(requestType == AuthRequestType.PreAuthorization)
		{
			totalAmount = new BigDecimal("0.00");
		}
		else
		{
			totalAmount = transactionDetail.getTransactionAmount();
		}

		return AmountDetails.builder()
				.totalAmount(totalAmount)
				.currency(Currency.USD.toString()) // TODO: Need to make sure this is fetched from merchant master, later release
				.build();
	}

	private BillTo createBillingAddress(final AuthReqAddressInfo addressInfo)
	{
		return BillTo.builder()
				.firstName(addressInfo.getBillToFirstName())
				.lastName(addressInfo.getBillToLastName())
				.address1(addressInfo.getBillToAddress1())
				.locality(addressInfo.getBillToCity())
				.administrativeArea(addressInfo.getBillToState())
				.country(addressInfo.getBillToCountry())
				.postalCode(addressInfo.getBillToZipCode())
				.phoneNumber(addressInfo.getBillToPhoneNumber())
				.email(addressInfo.getBillToEmail())
				.build();
	}

	private String convertExpDate(YearMonth ym)
	{
		DateTimeFormatter f = DateTimeFormatter.ofPattern("MMyy");
		String formattedDate = ym.format(f);
		return formattedDate;
	}

	@Override
	public CSReversalRequest createReversalRequest(AuthRequest gpasRequest, CybersourceAuthResponse cybersourceResponse)
	{
		AuthRequestType requestType = gpasRequest.getTransactionHeader().getRequestType();
		val transactionDetail = gpasRequest.getTransactionDetail();

		final ClientReferenceInformation clientInfo = addClientInfo(cybersourceResponse);
		final ReversalInformation reversalInfo = constructReversalInfo(requestType, transactionDetail);

		return CSReversalRequest.builder()
				.clientReferenceInformation(clientInfo)
				.reversalInformation(reversalInfo)
				.build();
	}

	private ReversalInformation constructReversalInfo(AuthRequestType requestType, AuthReqTransactionDetail transactionDetail)
	{
		final AmountDetails amountDetails = addAmountDetails(requestType, transactionDetail);

		return ReversalInformation.builder()
				.amountDetails(amountDetails)
				.reason("reversing payment")
				.build();
	}

	private ClientReferenceInformation addClientInfo(CybersourceAuthResponse cybersourceResponse)
	{
		return ClientReferenceInformation.builder()
				.code(cybersourceResponse.getClientReferenceCode())
				.build();
	}
}