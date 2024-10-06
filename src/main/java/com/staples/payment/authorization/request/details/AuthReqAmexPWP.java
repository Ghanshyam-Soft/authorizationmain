package com.staples.payment.authorization.request.details;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Schema(name = "RequestAmexPWP")
public class AuthReqAmexPWP
{
	@JsonProperty(value = "RequestID")
	@NotBlank
	@Size(max = 32, message = "AuthReqAmexPWP->RequestID, length cannot be more than 32 character!")
	String requestID;

	@JsonProperty(value = "CurrencyAmount")
	@Digits(integer = 8, fraction = 2)
	@DecimalMax(value = "99999999.99", message = "AuthReqAmexPWP->CurrencyAmount, must not be greater than 99999999.99")
	@Positive(message = "AuthReqAmexPWP->CurrencyAmount, must be a positive amount")
	BigDecimal currencyAmount;

	@JsonProperty(value = "RewardsAmount")
	@Digits(integer = 8, fraction = 2)
	@DecimalMax(value = "99999999.99", message = "AuthReqAmexPWP->RewardsAmount, must not be greater than 99999999.99")
	@Positive(message = "AuthReqAmexPWP->RewardsAmount, must be a positive amount")
	BigDecimal rewardsAmount;

	@JsonProperty(value = "RewardsUnit")
	String rewardsUnit; // TODO: Add max and min length validation

	@JsonProperty(value = "RewardsPointsRedeemed")
	@Min(value = 1, message = "rewardsPointsRedeemed must be greater than equal to 1")
	@Max(value = 999999999, message = "rewardsPointsRedeemed must be less than equal to 999999999")
	Long rewardsPointsRedeemed;
}
