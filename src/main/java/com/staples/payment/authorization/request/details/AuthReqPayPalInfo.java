package com.staples.payment.authorization.request.details;

import javax.validation.constraints.Email;
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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "RequestPayPalInfo")
public class AuthReqPayPalInfo
{
	@Size(max = 150, message = "RequestPayPalInfo->DeviceData, maximum 150 characters allowed")
	@JsonProperty(value = "DeviceData")
	String deviceData;

	@Email
	@JsonProperty(value = "PayerEmail")
	@Size(max = 128, message = "RequestPayPalInfo->PayerEmail, length cannot be more than 128 character!")
	@Schema(description = "If present, must either be null or a valid email address.")
	String payerEmail;
}
