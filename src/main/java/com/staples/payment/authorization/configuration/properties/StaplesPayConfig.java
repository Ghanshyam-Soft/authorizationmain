package com.staples.payment.authorization.configuration.properties;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties("stpay")
@ConstructorBinding
@lombok.Value
public class StaplesPayConfig
{
	String merchantId;
	String authSecret;
	String authKey;
	URI authTokenUrl;
	URI threeDSUrl;
	Duration timeout;
	int expireIn;
}