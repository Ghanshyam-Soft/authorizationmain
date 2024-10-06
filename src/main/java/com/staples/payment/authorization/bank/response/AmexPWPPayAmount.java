package com.staples.payment.authorization.bank.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class AmexPWPPayAmount
{
	@JsonProperty("Value")
	BigDecimal value;

	@JsonProperty("CurrencyCode")
	String currencyCode;
}
