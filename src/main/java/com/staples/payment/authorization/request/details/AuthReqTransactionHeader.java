package com.staples.payment.authorization.request.details;

import java.time.Instant;
import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.Country;
import com.staples.payment.shared.constant.Currency;
import com.staples.payment.shared.constant.PaymentType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "RequestTransactionHeader")
public class AuthReqTransactionHeader
{
	@JsonProperty(value = "BusinessUnit")
	@Size(max = 3, message = "TransactionHeader->BusinessUnit, length cannot be more than 3 character")
	@NotBlank(message = "TransactionHeader->BusinessUnit, cannot be null or empty!")
	String businessUnit;

	@JsonProperty(value = "ChildGUID")
	@Size(max = 32, message = "TransactionHeader->ChildGUID, length cannot be more than 32 character")
	@NotBlank(message = "TransactionHeader->ChildGUID, cannot be null or empty!")
	@Schema(description = "ChildGUID must be unique for every single request.")
	String childGUID;

	@JsonProperty(value = "ClientReferenceKey")
	@Size(max = 32, message = "TransactionHeader->ClientReferenceKey, length cannot be more than 32 character")
	@NotBlank(message = "TransactionHeader->ClientReferenceKey, cannot be null or empty!")
	String clientReferenceKey;

	@JsonProperty(value = "Country")
	@NotNull(message = "TransactionHeader->Country, cannot be null or empty!")
	Country country;

	@JsonProperty(value = "Currency")
	Currency currency;

	@JsonProperty(value = "Division")
	@Size(max = 3, message = "TransactionHeader->Division, length cannot be more than 3 character")
	@NotBlank(message = "TransactionHeader->Division, cannot be null or empty!")
	String division;

	@JsonProperty(value = "UTCDateTime")
	@NotNull(message = "TransactionHeader->UTCDateTime, cannot be null!")
	Instant utcDateTime;

	@JsonProperty(value = "LocalDateTime")
	@NotNull(message = "TransactionHeader->LocalDateTime, cannot be null!")
	@Schema(example = "2021-02-21T18:51:03.793")
	LocalDateTime localDateTime;

	@JsonProperty(value = "OriginatingGUID")
	@Size(max = 32, message = "TransactionHeader->OriginatingGUID, length cannot be more than 32 character")
	@Schema(description = "This is required for Lookups. It is also needed for any systematic retry (if you retry a request, not for reauths).") // It is validated in RequestBeanValidationImpl
	String originatingGUID;

	@JsonProperty(value = "ParentGUID")
	@Size(max = 100, message = "TransactionHeader->ParentGUID, length cannot be more than 100 character")
	@NotBlank(message = "TransactionHeader->ParentGUID, cannot be null or empty!")
	String parentGUID;

	@JsonProperty(value = "AuthReferenceGUID")
	@Size(min = 1, max = 32, message = "TransactionHeader->AuthReferenceGUID, length cannot be more than 32 character")
	@Schema(description = "This is required for Gift Cards only. Any AuthorizationComplete needs to have this same as ChildGUID of corresponding Authorization.")
	String authReferenceGUID;

	@JsonProperty(value = "PaymentType")
	@NotNull(message = "TransactionHeader->PaymentType, cannot be null or empty!")
	PaymentType paymentType;

	@JsonProperty(value = "RequestType")
	@NotNull(message = "TransactionHeader->RequestType, cannot be null or empty!")
	AuthRequestType requestType;

	@JsonProperty(value = "ReversalGUID")
	@Size(max = 32, message = "TransactionHeader->ReversalGUID, length cannot be more than 32 character")
	@Schema(description = "This is required for Reversals and Partial Reversals.") // It is validated in RequestBeanValidationImpl
	String reversalGUID;

	@JsonProperty(value = "OrderNumber")
	@Size(max = 10, message = "TransactionHeader->orderNumber, length cannot be more than 10 character")
	@Schema(description = "This is required for all non-PreAuths.") // It is validated in RequestBeanValidationImpl
	String orderNumber;

	@JsonProperty(value = "StoreNumber")
	@Size(max = 5, message = "TransactionHeader->StoreNumber, length cannot be more than 5 character")
	String storeNumber;

	@JsonProperty(value = "StoreRegNumber")
	@Size(max = 3, message = "TransactionHeader->StoreRegNumber, length cannot be more than 3 character")
	String storeRegNumber;

	@JsonProperty(value = "StoreTransNumber")
	@Size(max = 6, message = "TransactionHeader->StoreTransNumber, length cannot be more than 5 character")
	String storeTransNumber;
}