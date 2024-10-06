package com.staples.payment.authorization.response.details;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
		"balancerewardsAmount",
		"balancerewardsUnit",
		"balancecurrencyAmount",
		"balancecurrencyCode"
})
@Schema(name = "ResponsePWPBalanceInfo")
public class AuthRespBalanceInfo
{
	@JsonProperty(value = "BalanceRewardsAmount", required = true)
	BigDecimal balanceRewardsAmount;

	@JsonProperty(value = "BalanceRewardsUnit", required = true)
	String balanceRewardsUnit;

	@JsonProperty(value = "BalanceCurrencyAmount", required = true)
	BigDecimal balanceCurrencyAmount;

	@JsonProperty(value = "BalanceCurrencyCode", required = true)
	String balanceCurrencyCode;
}