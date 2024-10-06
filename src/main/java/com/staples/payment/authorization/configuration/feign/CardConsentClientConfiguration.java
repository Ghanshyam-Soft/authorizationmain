package com.staples.payment.authorization.configuration.feign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import feign.auth.BasicAuthRequestInterceptor;

public class CardConsentClientConfiguration
{
	@Bean
	BasicAuthRequestInterceptor basicAuthRequestInterceptor(@Value("${cardconsent.service.username}") String username, @Value("${cardconsent.service.password}") String password)
	{
		return new BasicAuthRequestInterceptor(username, password);
	}
}