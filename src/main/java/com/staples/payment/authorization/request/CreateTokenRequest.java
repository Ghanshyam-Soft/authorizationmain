package com.staples.payment.authorization.request;

import java.time.Instant;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.staples.payment.authorization.constant.TokenPaymentType;
import com.staples.payment.authorization.constant.TokenRequestType;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateTokenRequest
{
	@JsonProperty(value = "PaymentType")
	@NotNull(message = "TokenHeader->PaymentType, cannot be null or empty!")
	TokenPaymentType paymentType;

	@JsonProperty(value = "RequestType")
	@NotNull(message = "TokenHeader->RequestType, cannot be null or empty!")
	TokenRequestType requestType;

	@JsonProperty(value = "CustomerId")
	@Size(max = 36, message = "TokenHeader->CustomerId, maximum 36 characters allowed")
	String customerId;

	@JsonProperty(value = "BusinessUnit")
	@Size(max = 3, message = "TokenHeader->BusinessUnit, length cannot be more than 3 character")
	@NotBlank(message = "TokenHeader->BusinessUnit, cannot be null or empty!")
	String businessUnit;

	@JsonProperty(value = "Division")
	@Size(max = 3, message = "TokenHeader->Division, length cannot be more than 3 character")
	@NotBlank(message = "tokenHeader->Division, cannot be null or empty!")
	String division;

	@JsonProperty(value = "UTCDateTime")
	@NotNull(message = "TokenHeader->UTCDateTime, cannot be null!")
	Instant utcDateTime;
}