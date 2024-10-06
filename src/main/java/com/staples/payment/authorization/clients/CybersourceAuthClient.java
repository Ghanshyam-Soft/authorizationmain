package com.staples.payment.authorization.clients;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.staples.payment.authorization.configuration.feign.CybersourceFeignClientConfiguration;
import com.staples.payment.shared.cybersource.dto.request.CSCreditRequest;
import com.staples.payment.shared.cybersource.dto.request.CSPaymentsRequest;
import com.staples.payment.shared.cybersource.dto.request.CSReversalRequest;
import com.staples.payment.shared.cybersource.dto.response.CSCreditResponse;
import com.staples.payment.shared.cybersource.dto.response.CSPaymentsResponse;
import com.staples.payment.shared.cybersource.dto.response.CSReversalsResponse;
import com.staples.payment.shared.cybersource.dto.response.ResponseWrapper;

@FeignClient(name = "cyb-auth-client", url = "${cyb.service.baseUrl}", path = "/gpas/cybersource", configuration = CybersourceFeignClientConfiguration.class)
public interface CybersourceAuthClient
{
	@PostMapping("/payments")
	ResponseWrapper<CSPaymentsResponse> payments(@RequestHeader Map<String, String> headers, @RequestBody CSPaymentsRequest request);

	@PostMapping("/payments/{id}/reversals")
	ResponseWrapper<CSReversalsResponse> reversals(@RequestHeader Map<String, String> headers, @RequestBody CSReversalRequest request, @PathVariable("id") String id);

	@PostMapping("/credits")
	ResponseWrapper<CSCreditResponse> credits(@RequestHeader Map<String, String> headers, @RequestBody CSCreditRequest request);

}