package com.staples.payment.authorization.service.impl;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.staples.payment.authorization.clients.BraintreeAuthClient;
import com.staples.payment.authorization.request.CreateTokenRequest;
import com.staples.payment.authorization.response.CreateTokenResponse;
import com.staples.payment.authorization.service.CreateTokenService;
import com.staples.payment.shared.braintree.request.TokenRequest;
import com.staples.payment.shared.braintree.response.TokenResponse;
import com.staples.payment.shared.cache.BusinessMasterCache;
import com.staples.payment.shared.constant.Bank;
import com.staples.payment.shared.constant.GpasRespCode;
import com.staples.payment.shared.constant.PaymentMethod;
import com.staples.payment.shared.entity.MerchantMaster;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CreateTokenServiceImpl implements CreateTokenService
{
	private final BraintreeAuthClient braintreeAuthClient;
	private final BusinessMasterCache businessMasterCache;

	public CreateTokenServiceImpl(BraintreeAuthClient braintreeAuthClient, BusinessMasterCache businessMasterCache)
	{
		super();
		this.braintreeAuthClient = braintreeAuthClient;
		this.businessMasterCache = businessMasterCache;
	}

	@Override
	public CreateTokenResponse processCreateTokenRequest(CreateTokenRequest request, String internalRequestId)
	{
		final PaymentMethod paymentMethod = request.getPaymentType().getPaymentMethod();
		MerchantMaster merchantMaster = retrieveMerchantMaster(request.getBusinessUnit(), request.getDivision(), paymentMethod);

		final Bank bank = merchantMaster.getBankName();

		if(bank == Bank.BRAINTREE)
		{
			TokenRequest tokenRequest = TokenRequest.builder()
					.requestId(internalRequestId)
					.customerId(request.getCustomerId())
					.build();

			TokenResponse response = braintreeAuthClient.clientToken(tokenRequest);
			return createTokenResponse(request, response);
		}
		else
		{
			throw new RuntimeException("Invalid bank for CreateTokenEndpoint");
		}
	}

	private CreateTokenResponse createTokenResponse(CreateTokenRequest request, @Nullable TokenResponse tokenResponse)
	{
		if(tokenResponse != null && tokenResponse.getClientToken() != null)
		{
			return CreateTokenResponse.builder()
					.clientToken(tokenResponse.getClientToken())
					.businessUnit(request.getBusinessUnit())
					.division(request.getDivision())
					.customerId(request.getCustomerId())
					.paymentType(request.getPaymentType())
					.requestType(request.getRequestType())
					.responseCode(GpasRespCode.A)
					.reasonCode("00")
					.build();
		}
		else
		{
			log.debug("Customer ID does not exist");
			return CreateTokenResponse.builder()
					.clientToken(null)
					.businessUnit(request.getBusinessUnit())
					.division(request.getDivision())
					.customerId(request.getCustomerId())
					.paymentType(request.getPaymentType())
					.requestType(request.getRequestType())
					.responseCode(GpasRespCode.S)
					.reasonCode("91")
					.build();
		}
	}

	private MerchantMaster retrieveMerchantMaster(String businessUnit, String division, PaymentMethod paymentType)
	{
		return businessMasterCache.getMerchantMasterBy(businessUnit, division, paymentType); // TODO: Change if switch caching methods
	}
}
