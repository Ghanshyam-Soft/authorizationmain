package com.staples.payment.authorization.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.staples.payment.authorization.configuration.feign.BraintreeFeignClientConfiguration;
import com.staples.payment.shared.braintree.request.SaleRequest;
import com.staples.payment.shared.braintree.request.TokenRequest;
import com.staples.payment.shared.braintree.request.VoidRequest;
import com.staples.payment.shared.braintree.response.TransactionResponse;
import com.staples.payment.shared.braintree.response.TokenResponse;

@FeignClient(name = "braintree-auth-client", url = "${braintree.service.baseUrl}", path = "/gpas/braintree", configuration = BraintreeFeignClientConfiguration.class)
public interface BraintreeAuthClient
{
	@PostMapping("/sale")
	TransactionResponse sale(SaleRequest request);

	@PostMapping("/token")
	TokenResponse clientToken(TokenRequest request);

	@PostMapping("/void")
	TransactionResponse voidTransaction(VoidRequest request);
}