package com.staples.payment.authorization.bank.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class AmexPWPEmbedded
{
	@JsonProperty("CurrentBalance")
	BigDecimal currentBalance;

	@JsonProperty("AmountBalance")
	BigDecimal amountBalance;

	@JsonProperty("TierCode")
	String tierCode;
}
