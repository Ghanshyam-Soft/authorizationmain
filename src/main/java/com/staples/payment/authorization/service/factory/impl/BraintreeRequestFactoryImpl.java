package com.staples.payment.authorization.service.factory.impl;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.details.AuthReqAddressInfo;
import com.staples.payment.authorization.service.factory.BraintreeRequestFactory;
import com.staples.payment.shared.braintree.request.SaleRequest;
import com.staples.payment.shared.braintree.request.VoidRequest;
import com.staples.payment.shared.braintree.request.detail.Address;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.braintree.BraintreeResponse;

import lombok.val;

@Service
public class BraintreeRequestFactoryImpl implements BraintreeRequestFactory
{
	@Override
	public SaleRequest createSaleRequest(AuthRequest gpasRequest, AuthLog authLog)
	{
		val transactionDetail = gpasRequest.getTransactionDetail();
		val transactionHeader = gpasRequest.getTransactionHeader();
		val payPalInfo = gpasRequest.getPayPalInfo();
		val addressInfo = gpasRequest.getAddressInfo();

		boolean createCustomer = (null != transactionDetail.getCreateCustomer()) ? transactionDetail.getCreateCustomer() : false;

		final Address shippingAddress = createShippingAddress(addressInfo);
		final Address billingAddress = createBillingAddress(addressInfo);

		SaleRequest request = SaleRequest.builder()
				.amount(transactionDetail.getTransactionAmount())
				.customerId(transactionDetail.getCustomerId())
				.createCustomer(createCustomer) // for creating the new customer that does not exist in braintree end
				.deviceData(payPalInfo.getDeviceData())
				.orderId(transactionHeader.getOrderNumber())
				.payerEmail(payPalInfo.getPayerEmail())
				.paymentMethodToken(transactionDetail.getPaymentToken())
				.paymentMethodNonce(transactionDetail.getTemporaryPaymentToken())
				.shippingAddress(shippingAddress)
				.billingAddress(billingAddress)
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
				.firstName(addressInfo.getShipToFirstName())
				.lastName(addressInfo.getShipToLastName())
				.phoneNumber(addressInfo.getShipToPhoneNumber())
				.streetAddress(addressInfo.getShipToAddress1())
				.extendedAddress(addressInfo.getShipToAddress2())
				.locality(addressInfo.getShipToCity())
				.region(addressInfo.getShipToState())
				.countryName(addressInfo.getShipToCountry())
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
				.firstName(addressInfo.getBillToFirstName())
				.lastName(addressInfo.getBillToLastName())
				.phoneNumber(addressInfo.getBillToPhoneNumber())
				.streetAddress(addressInfo.getBillToAddress1())
				.extendedAddress(addressInfo.getBillToAddress2())
				.locality(addressInfo.getBillToCity())
				.region(addressInfo.getBillToState())
				.countryName(addressInfo.getBillToCountry())
				.postalCode(addressInfo.getBillToZipCode())
				.build();
	}

	@Override
	public VoidRequest createVoidRequest(BraintreeResponse braintreeResponse)
	{
		VoidRequest request = VoidRequest.builder()
				.transactionId(braintreeResponse.getTransactionId())
				.gpasKey(braintreeResponse.getGpasKey()) // custom field
				.build();

		return request;
	}
}