package com.staples.payment.authorization.configuration.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@ConfigurationProperties(prefix = "default.approval")
@Configuration
@Data
public class DefaultApprovalConfig
{
	private String descriptionText;
	private String vendorInfo;
	private String gpasApprovalReasonCode;
	private String authCode;
	private List<Consumer> consumerList;

	@Data
	public static class Consumer
	{
		private String division;
		private String businessUnit;
	}
}
