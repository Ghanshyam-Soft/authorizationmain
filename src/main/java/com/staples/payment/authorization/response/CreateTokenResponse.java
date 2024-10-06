package com.staples.payment.authorization.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.staples.payment.authorization.constant.TokenPaymentType;
import com.staples.payment.authorization.constant.TokenRequestType;
import com.staples.payment.shared.constant.GpasRespCode;

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
		"paymentType",
		"requestType",
		"customerId",
		"businessUnit",
		"division",
		"clientToken",
		"responseCode",
		"reasonCode",
		"vendorResponseCode"
})
public class CreateTokenResponse
{
	@JsonProperty(value = "PaymentType", required = true)
	@Schema(required = true)
	TokenPaymentType paymentType;

	@JsonProperty(value = "RequestType", required = true)
	@Schema(required = true)
	TokenRequestType requestType;

	@JsonProperty(value = "CustomerId", required = true)
	@Schema(required = true)
	String customerId;

	@JsonProperty(value = "BusinessUnit", required = true)
	@Schema(required = true)
	String businessUnit;

	@JsonProperty(value = "Division", required = true)
	@Schema(required = true)
	String division;

	@JsonProperty(value = "ClientToken", required = true)
	@Schema(required = true)
	String clientToken;

	@JsonProperty(value = "ResponseCode", required = true)
	@Schema(required = true)
	GpasRespCode responseCode;

	@JsonProperty(value = "ReasonCode", required = true)
	@Schema(required = true, description = "The various values span a range from 00 to 99, with all being strings made of two digits.")
	String reasonCode;

	@JsonProperty(value = "VendorResponseCode", required = true)
	@Schema(required = true)
	String vendorResponseCode;
}