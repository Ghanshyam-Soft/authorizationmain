package com.staples.payment.authorization.configuration.feign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import feign.auth.BasicAuthRequestInterceptor;

public class BraintreeFeignClientConfiguration
{
	@Bean
	BasicAuthRequestInterceptor basicAuthRequestInterceptor(@Value("${braintree.service.username}") String username, @Value("${braintree.service.password}") String password)
	{
		return new BasicAuthRequestInterceptor(username, password);
	}
}