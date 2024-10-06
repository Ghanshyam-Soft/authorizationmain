package com.staples.payment.authorization.configuration.feign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import feign.auth.BasicAuthRequestInterceptor;

public class CybersourceFeignClientConfiguration
{
	@Bean
	BasicAuthRequestInterceptor basicAuthRequestInterceptor(@Value("${cyb.service.username}") String username, @Value("${cyb.service.password}") String password)
	{
		return new BasicAuthRequestInterceptor(username, password);
	}
}