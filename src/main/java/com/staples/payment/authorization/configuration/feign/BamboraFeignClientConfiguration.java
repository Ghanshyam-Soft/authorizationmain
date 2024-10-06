package com.staples.payment.authorization.configuration.feign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import feign.auth.BasicAuthRequestInterceptor;

public class BamboraFeignClientConfiguration
{
	@Bean
	BasicAuthRequestInterceptor basicAuthRequestInterceptor(@Value("${bambora.service.username}") String username, @Value("${bambora.service.password}") String password)
	{
		return new BasicAuthRequestInterceptor(username, password);
	}
}