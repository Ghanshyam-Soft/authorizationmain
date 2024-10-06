package com.staples.payment.authorization.request;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.staples.payment.authorization.request.details.AuthReqAddressInfo;
import com.staples.payment.authorization.request.details.AuthReqAmexPWP;
import com.staples.payment.authorization.request.details.AuthReqCOFInfo;
import com.staples.payment.authorization.request.details.AuthReqCardInfo;
import com.staples.payment.authorization.request.details.AuthReqPayPalInfo;
import com.staples.payment.authorization.request.details.AuthReqTransactionDetail;
import com.staples.payment.authorization.request.details.AuthReqTransactionHeader;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthRequest
{
	@JsonProperty(value = "TransactionHeader")
	@Valid
	@NotNull(message = "TransactionHeader must not be null or empty!")
	AuthReqTransactionHeader transactionHeader;

	@JsonProperty(value = "TransactionDetail")
	@Valid
	@NotNull(message = "TransactionDetail must not be null or empty!")
	AuthReqTransactionDetail transactionDetail;

	@JsonProperty(value = "AddressInfo")
	@Valid
	AuthReqAddressInfo addressInfo;

	@JsonProperty(value = "AmexPWP")
	@Valid
	AuthReqAmexPWP amexPWP;

	@JsonProperty(value = "COFInfo")
	@Valid
	AuthReqCOFInfo cofInfo;

	@JsonProperty(value = "CardInfo")
	@Valid
	@NotNull(message = "CardInfo must not be null!")
	AuthReqCardInfo cardInfo;

	@JsonProperty(value = "PayPalInfo")
	@Valid
	AuthReqPayPalInfo payPalInfo;
}