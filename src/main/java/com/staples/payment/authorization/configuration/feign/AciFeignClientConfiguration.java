package com.staples.payment.authorization.configuration.feign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import feign.auth.BasicAuthRequestInterceptor;

public class AciFeignClientConfiguration
{
	@Bean
	BasicAuthRequestInterceptor basicAuthRequestInterceptor(@Value("${aci.service.username}") String username, @Value("${aci.service.password}") String password)
	{
		return new BasicAuthRequestInterceptor(username, password);
	}
}