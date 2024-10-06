package com.staples.payment.authorization.configuration.feign;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Retryer;

@Configuration
public class FeignConfig
{
	private final long period;
	private final long maxPeriod;
	private final int maxAttempts;

	public FeignConfig(
			@Value("${feign.retry.period}") long period,
			@Value("${feign.retry.maxPeriod}") long maxPeriod,
			@Value("${feign.retry.maxAttempts}") int maxAttempts)
	{
		this.period = period;
		this.maxPeriod = maxPeriod;
		this.maxAttempts = maxAttempts;
	}

	@Bean
	Retryer feignRetryer()
	{
		return new Retryer.Default(period, SECONDS.toMillis(maxPeriod), maxAttempts);
	}
}
