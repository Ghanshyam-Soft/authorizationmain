package com.staples.payment.authorization.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.staples.payment.authorization.response.details.AuthRespAmexPWP;
import com.staples.payment.authorization.response.details.AuthRespCOFInfo;
import com.staples.payment.authorization.response.details.AuthRespPayPalInfo;
import com.staples.payment.authorization.response.details.AuthRespTransactionDetail;
import com.staples.payment.authorization.response.details.AuthRespTransactionHeader;

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
		"transactionHeader",
		"transactionDetail",
		"amexPWP",
		"payPalInfo"
})
public class AuthResponse
{
	@JsonProperty(value = "TransactionHeader", required = true)
	AuthRespTransactionHeader transactionHeader;

	@JsonProperty(value = "TransactionDetail", required = true)
	AuthRespTransactionDetail transactionDetail;

	@JsonProperty(value = "AmexPWP")
	AuthRespAmexPWP amexPWP;

	@JsonProperty(value = "PayPalInfo")
	AuthRespPayPalInfo payPalInfo;

	@JsonProperty(value = "COFInfo")
	AuthRespCOFInfo cofInfo;
}