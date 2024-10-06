package com.staples.payment.authorization.service.factory.impl;

import java.math.BigDecimal;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.details.AuthReqAddressInfo;
import com.staples.payment.authorization.service.factory.BamboraRequestFactory;
import com.staples.payment.shared.bambora.request.SaleRequest;
import com.staples.payment.shared.bambora.request.detail.Address;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.PaymentMethod;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.MerchantMaster;

import lombok.val;

@Service
public class BamboraRequestFactoryImpl implements BamboraRequestFactory
{
	@Override
	public SaleRequest createSaleRequest(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog)
	{
		val transactionDetail = gpasRequest.getTransactionDetail();
		val transactionHeader = gpasRequest.getTransactionHeader();
		val addressInfo = gpasRequest.getAddressInfo();
		String uniqueOrderNumber = authLog.getGpasKey().substring(3);

		final BigDecimal transactionAmount;
		if(transactionHeader.getRequestType() == AuthRequestType.PreAuthorization)
		{
			if(gpasRequest.getCardInfo().getPaymentType() == PaymentMethod.AM)
			{
				transactionAmount = BigDecimal.ONE; // TODO: Should this be 1.00 instead of 1?
			}
			else
			{
				transactionAmount = new BigDecimal("0.01");
			}
		}
		else
		{
			transactionAmount = transactionDetail.getTransactionAmount();
		}

		Address shippingAddress = createShippingAddress(addressInfo);
		Address billingAddress = createBillingAddress(addressInfo);

		final SaleRequest request = SaleRequest.builder()
				.amount(transactionAmount)
				.merchantId(merchantMaster.getBankMerchantId())
				.orderNumber(uniqueOrderNumber)
				.paymentMethod("card")
				.shipping(shippingAddress)
				.billing(billingAddress)
				.paymentToken(transactionDetail.getPaymentToken())
				.expirationDate(gpasRequest.getCardInfo().getExpirationDate())
				.ccin(gpasRequest.getCardInfo().getCcin())
				.gpasKey(authLog.getGpasKey()) // custom field
				.build();

		return request;
	}

	private @Nullable Address createShippingAddress(@Nullable final AuthReqAddressInfo addressInfo)
	{
		if(addressInfo == null)
		{
			return null;
		}

		return Address.builder()
				.name(addressInfo.getShipToFirstName() + " " + addressInfo.getShipToLastName())
				.phoneNumber(addressInfo.getShipToPhoneNumber())
				.addressLine1(addressInfo.getShipToAddress1())
				.addressLine2(addressInfo.getShipToAddress2())
				.city(addressInfo.getShipToCity())
				.province(addressInfo.getShipToState())
				.country(addressInfo.getShipToCountry())
				.postalCode(addressInfo.getShipToZipCode())
				.build();
	}

	private @Nullable Address createBillingAddress(@Nullable final AuthReqAddressInfo addressInfo)
	{
		if(addressInfo == null)
		{
			return null;
		}

		return Address.builder()
				.name(addressInfo.getBillToFirstName() + " " + addressInfo.getBillToLastName())
				.phoneNumber(addressInfo.getBillToPhoneNumber())
				.addressLine1(addressInfo.getBillToAddress1())
				.addressLine2(addressInfo.getBillToAddress2())
				.city(addressInfo.getBillToCity())
				.province(addressInfo.getBillToState())
				.country(addressInfo.getBillToCountry())
				.postalCode(addressInfo.getBillToZipCode())
				.emailAddress(addressInfo.getBillToEmail())
				.build();
	}
}