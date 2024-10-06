package com.staples.payment.authorization.response.details;

import javax.validation.constraints.Size;

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
		"sellerProtectionStatus",
		"customerId",
		"deviceData",
		"payerEmail",
		"payerPhone",
		"payerId",
		"payerStatus",
		"transactionId"
})
@Schema(name = "ResponsePayPalInfo")
public class AuthRespPayPalInfo
{
	@JsonProperty(value = "SellerProtectionStatus", required = true)
	@Schema(required = true)
	@Size(max = 12)
	String sellerProtectionStatus;

	@JsonProperty(value = "DeviceData", required = true)
	@Schema(required = true)
	@Size(max = 150)
	String deviceData;

	@JsonProperty(value = "PayerEmail", required = true)
	@Schema(required = true)
	@Size(max = 254)
	String payerEmail;

	@JsonProperty(value = "PayerPhone", required = true)
	@Schema(required = true)
	@Size(max = 17)
	String payerPhone;

	@JsonProperty(value = "PayerId", required = true)
	@Schema(required = true)
	@Size(max = 13)
	String payerId;

	@JsonProperty(value = "PayerStatus", required = true)
	@Schema(required = true)
	@Size(max = 10)
	String payerStatus;

	@JsonProperty(value = "TransactionId", required = true)
	@Schema(required = true)
	@Size(max = 64)
	String transactionId;
}
