package com.staples.payment.authorization.bank.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({
		"requestorOrderId",
		"accountKey",
		"amount",
		"conversionRate",
		"embedded",
		"response_status",
		"failureReason"
})
public class AmexPWPBalanceResponse
{
	@JsonProperty("RequestorOrderId")
	String requestorOrderId;

	@JsonProperty("AccountKey")
	AmexPWPAccountKey accountKey;

	@JsonProperty("Amount")
	AmexPWPPayAmount amount;

	@JsonProperty("ConversionRate")
	BigDecimal conversionRate;

	@JsonProperty("Embedded")
	AmexPWPEmbedded embedded;

	@JsonProperty("ResponseStatus")
	AmexResponseStatus responseStatus;

}
