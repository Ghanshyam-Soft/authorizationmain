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
		"rewardsAmount",
		"rewardsUnit",
		"currencyAmount",
		"currencyCode"
})
@Schema(name = "ResponsePWPReservationInfo")
public class AuthRespPWPReservationInfo
{
	@JsonProperty(value = "RewardsAmount", required = true)
	BigDecimal rewardsAmount;

	@JsonProperty(value = "RewardsUnit", required = true)
	String rewardsUnit;

	@JsonProperty(value = "CurrencyAmount", required = true)
	BigDecimal currencyAmount;

	@JsonProperty(value = "CurrencyCode", required = true)
	String currencyCode;
}