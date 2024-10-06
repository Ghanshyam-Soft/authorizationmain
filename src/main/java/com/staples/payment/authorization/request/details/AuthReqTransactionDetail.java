package com.staples.payment.authorization.request.details;

import java.math.BigDecimal;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
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
@Schema(name = "RequestTransactionDetail")
public class AuthReqTransactionDetail
{

	@JsonProperty(value = "PaymentToken")
	@Size(max = 64, message = "AuthReqTransactionDetail->PaymentToken, length cannot be more than 64 character")
	@Schema(description = "For PayPal this should be the paymentMethodToken, otherwise it is the AliasAccount. Either PaymentToken, or TemporaryPaymentToken is required.")
	String paymentToken;

	@JsonProperty(value = "TemporaryPaymentToken")
	@Size(max = 256, message = "AuthReqTransactionDetail->TemporaryPaymentToken, length cannot be more than 256 character")
	@Schema(description = "For PayPal this is the paymentMethodNonce otherwise it is the paymentReferenceToken. Either PaymentToken, or TemporaryPaymentToken is required.")
	String temporaryPaymentToken;

	@JsonProperty(value = "GiftCardNumber")
	@Size(min = 1, max = 19, message = "AuthReqTransactionDetail->GiftCardNumber, length cannot be more than 19 character")
	@Schema(description = "This is only required for Payment Type 'Gift Card'")
	String giftCardNumber;

	@JsonProperty(value = "BillTo")
	String billTo;// TODO: leaving without validation for now, but will eventually want length validation

	@JsonProperty(value = "MasterAccountNo")
	String masterAccountNo;// TODO: leaving without validation for now, but will eventually want length validation

	@JsonProperty(value = "CustomerId")
	@Size(max = 36)
	@Schema(description = "For MIT transactions, this needs to be the same customerId passed to the ConsentService when storing the Account.")
	String customerId;

	@JsonProperty(value = "ConsentClientId")
	@Schema(description = "The ClientId passed to the ConsentService when storing the Account.")
	String consentClientId; // TODO: Can we validate this? From the perspective of the CCS there is no maximum length. Rather it has a fixed number of valid values in the properties file.

	@JsonProperty(value = "CreateCustomer")
	Boolean createCustomer;

	@JsonProperty(value = "PONumber")
	@Size(max = 16, message = "AuthReqTransactionDetail->PONumber, length cannot be more than 16 character")
	String poNumber;

	@Valid
	@JsonProperty(value = "POSDataCode")
	@NotNull(message = "TransactionDetail->POSDataCode, cannot be null or empty!")
	PosDataCode posDataCode;

	@JsonProperty(value = "ReferenceToken")
	String referenceToken;// TODO: leaving without validation for now, but will eventually want length validation

	@JsonProperty(value = "SaleAmount")
	@NotNull(message = "AuthReqTransactionDetail->SaleAmount, cannot be null or empty!")
	@Digits(integer = 8, fraction = 2)
	@DecimalMax(value = "99999999.99", message = "AuthReqTransactionDetail->SaleAmount-View, decimal value should be inbetween 0.00 to 99999999.99")
	@DecimalMin(value = "0.00", message = "AuthReqTransactionDetail->SaleAmount-View, decimal value should be inbetween 0.00 to 99999999.99")
	BigDecimal saleAmount;

	@JsonProperty(value = "TransactionAmount")
	@NotNull(message = "AuthReqTransactionDetail->TransactionAmount, cannot be null or empty!")
	@Digits(integer = 8, fraction = 2)
	@DecimalMax(value = "99999999.99", message = "AuthReqTransactionDetail->SaleAmount-View, decimal value should be inbetween 0.00 to 99999999.99")
	@DecimalMin(value = "0.00", message = "AuthReqTransactionDetail->SaleAmount-View, decimal value should be inbetween 0.00 to 99999999.99")
	BigDecimal transactionAmount;
}