package com.staples.payment.authorization.service.factory;

import java.util.Map;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.shared.cybersource.dto.request.CSPaymentsRequest;
import com.staples.payment.shared.cybersource.dto.request.CSReversalRequest;
import com.staples.payment.shared.entity.MerchantMaster;
import com.staples.payment.shared.entity.cybersource.CybersourceAuthResponse;

public interface CybersourceRequestFactory
{
	Map<String, String> createHeaders(String gpasKey, MerchantMaster merchantMaster);

	CSPaymentsRequest createPaymentRequest(AuthRequest gpasRequest);

	CSReversalRequest createReversalRequest(AuthRequest gpasRequest, CybersourceAuthResponse cybersourceResponse);

}