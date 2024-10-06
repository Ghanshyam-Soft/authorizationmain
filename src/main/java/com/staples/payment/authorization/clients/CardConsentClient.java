package com.staples.payment.authorization.clients;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.staples.payment.authorization.configuration.feign.CardConsentClientConfiguration;
import com.staples.payment.authorization.dto.cardConsent.CardConsentResponse;

@FeignClient(name = "card-consent-client", url = "${cardconsent.service.baseUrl}", path = "/paymentservices", configuration = CardConsentClientConfiguration.class)
public interface CardConsentClient
{
	@GetMapping(value = "/storedcredentials/{clientId}/{customerId}/{businessUnit}/{division}")
	List<CardConsentResponse> getStoredCredentials(@PathVariable("clientId") String clientId, @PathVariable("customerId") String customerId,
			@PathVariable("businessUnit") String businessUnit, @PathVariable("division") String division);
}