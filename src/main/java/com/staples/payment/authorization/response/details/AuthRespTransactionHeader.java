package com.staples.payment.authorization.response.details;

import java.time.Instant;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.Country;
import com.staples.payment.shared.constant.Currency;
import com.staples.payment.shared.constant.PaymentType;

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
		"utcDateTime",
		"country",
		"currency",
		"businessUnit",
		"division",
		"orderNumber",
		"storeNumber",
		"storeRegNumber",
		"storeTransNumber",
		"parentGUID",
		"childGUID",
		"originatingGUID",
		"reversalGUID",
		"clientReferenceKey",
		"localDateTime"
})
@Schema(name = "ResponseTransactionHeader")
public class AuthRespTransactionHeader
{
	@JsonProperty(value = "PaymentType", required = true)
	@Schema(required = true)
	PaymentType paymentType;

	@JsonProperty(value = "RequestType", required = true)
	@Schema(required = true)
	AuthRequestType requestType;

	@JsonProperty(value = "UTCDateTime", required = true)
	@Schema(required = true)
	Instant utcDateTime;

	@JsonProperty(value = "Country", required = true)
	@Schema(required = true)
	Country country;

	@JsonProperty(value = "Currency", required = true)
	@Schema(required = true)
	Currency currency;

	@JsonProperty(value = "BusinessUnit", required = true)
	@Schema(required = true)
	String businessUnit;

	@JsonProperty(value = "Division", required = true)
	@Schema(required = true)
	String division;

	@JsonProperty(value = "OrderNumber", required = true)
	@Schema(required = true)
	String orderNumber;

	@JsonProperty(value = "StoreNumber", required = true)
	@Schema(required = true)
	String storeNumber;

	@JsonProperty(value = "StoreRegNumber", required = true)
	@Schema(required = true)
	String storeRegNumber;

	@JsonProperty(value = "StoreTransNumber", required = true)
	@Schema(required = true)
	String storeTransNumber;

	@JsonProperty(value = "ParentGUID", required = true)
	@Schema(required = true)
	String parentGUID;

	@JsonProperty(value = "ChildGUID", required = true)
	@Schema(required = true)
	String childGUID;

	@JsonProperty(value = "OriginatingGUID", required = true)
	@Schema(required = true)
	String originatingGUID;

	@JsonProperty(value = "ReversalGUID", required = true)
	@Schema(required = true)
	String reversalGUID;

	@JsonProperty(value = "ClientReferenceKey", required = true)
	@Schema(required = true)
	String clientReferenceKey;

	@JsonProperty(value = "LocalDateTime", required = true)
	@Schema(required = true, example = "2021-02-21T18:51:03.793")
	LocalDateTime localDateTime;
}