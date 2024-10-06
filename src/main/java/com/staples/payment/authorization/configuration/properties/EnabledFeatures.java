package com.staples.payment.authorization.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties("feature.enable")
@ConstructorBinding
@lombok.Value
public class EnabledFeatures
{
	boolean cit;
	boolean mit;
	boolean threeDs;
	boolean pwpBalance;
	boolean pwpRedeem;
	boolean prepaid;
	boolean giftcard;
}