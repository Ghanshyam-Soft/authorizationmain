package com.staples.payment.authorization.configuration.properties;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@ConfigurationProperties(prefix = "fraud.check")
@Configuration
@Data
public class FraudCheckConfig
{
	private boolean enabled;
	private String descriptionText;
	private String vendorInfo;
	private String gpasFraudReasonCode;
	private List<ConsumerDetail> consumerDetails;

	@Data
	public static class ConsumerDetail
	{
		private String division;
		private String businessUnit;
		private Duration duration;
		private Integer retryCount;
	}
}
