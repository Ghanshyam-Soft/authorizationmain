package com.staples.payment.authorization.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.staples.payment.authorization.configuration.feign.AciFeignClientConfiguration;
import com.staples.payment.shared.aci.request.AuthorizationCompleteRequest;
import com.staples.payment.shared.aci.request.AuthorizationRequest;
import com.staples.payment.shared.aci.request.BalanceRequest;
import com.staples.payment.shared.aci.request.CardVerificationRequest;
import com.staples.payment.shared.aci.request.PartialReversalRequest;
import com.staples.payment.shared.aci.request.RefundRequest;
import com.staples.payment.shared.aci.request.ReversalRequest;
import com.staples.payment.shared.aci.response.AciBalanceResponse;
import com.staples.payment.shared.aci.response.AciPaymentResponse;
import com.staples.payment.shared.aci.response.ResponseWrapper;

@FeignClient(name = "aci-auth-client", url = "${aci.service.baseUrl}", path = "/gpas/aci", configuration = AciFeignClientConfiguration.class)
public interface AciAuthClient
{
	@PostMapping("/cardVerification")
	ResponseWrapper<AciPaymentResponse> cardVerification(CardVerificationRequest request);

	@PostMapping("/authorization")
	ResponseWrapper<AciPaymentResponse> authorization(AuthorizationRequest request);

	@PostMapping("/authorizationComplete")
	ResponseWrapper<AciPaymentResponse> authorizationComplete(AuthorizationCompleteRequest request);

	@PostMapping("/refund")
	ResponseWrapper<AciPaymentResponse> refund(RefundRequest request);

	@PostMapping("/reversal")
	ResponseWrapper<AciPaymentResponse> reversal(ReversalRequest request);

	@PostMapping("/partialReversal")
	ResponseWrapper<AciPaymentResponse> partialReversal(PartialReversalRequest request);

	@PostMapping("/balanceInquiry")
	ResponseWrapper<AciBalanceResponse> balanceInquiry(BalanceRequest request);
}