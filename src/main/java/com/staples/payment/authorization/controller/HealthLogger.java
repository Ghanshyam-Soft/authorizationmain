package com.staples.payment.authorization.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class HealthLogger
{
	private static final Logger audit = LoggerFactory.getLogger("audit-log");

	private final String version;

	public HealthLogger(@Value("${app.version}") String version)
	{
		this.version = version;
	}

	@Scheduled(fixedRateString = "${health.logging.fixed.rate}")
	public void logHealth()
	{
		audit.info("Service is UP. Version is " + version);
	}
}
