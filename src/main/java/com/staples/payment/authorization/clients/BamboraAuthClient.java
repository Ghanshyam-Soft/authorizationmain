package com.staples.payment.authorization.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.staples.payment.authorization.configuration.feign.BamboraFeignClientConfiguration;
import com.staples.payment.shared.bambora.request.SaleRequest;
import com.staples.payment.shared.bambora.response.BamboraResponseWrapper;

@FeignClient(name = "bambora-auth-client", url = "${bambora.service.baseUrl}", path = "/gpas/bambora", configuration = BamboraFeignClientConfiguration.class)
public interface BamboraAuthClient
{
	@PostMapping("/sale")
	BamboraResponseWrapper sale(SaleRequest request);
}