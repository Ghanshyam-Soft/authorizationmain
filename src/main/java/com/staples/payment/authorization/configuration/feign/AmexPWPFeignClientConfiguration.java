package com.staples.payment.authorization.configuration.feign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import feign.auth.BasicAuthRequestInterceptor;

public class AmexPWPFeignClientConfiguration
{
	@Bean
	BasicAuthRequestInterceptor basicAuthRequestInterceptor(@Value("${amexpwp.service.username}") String username, @Value("${amexpwp.service.password}") String password)
	{
		return new BasicAuthRequestInterceptor(username, password);
	}
}