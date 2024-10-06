package com.staples.payment.authorization.request.details;

import java.time.YearMonth;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.staples.payment.shared.constant.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "RequestCardInfo")
public class AuthReqCardInfo // Must generally match DPSWS response
{
	// TODO: Temporarily commenting out these fields to ensure we don't get errors in prod. Need to discover any problems with fields.

	/*	@JsonProperty(value = "BankID")
		@Size(max = 4, message = "AuthReqCardInfo - BankID, length cannot be more than 4 character")
		String bankID; //TODO: We are getting longer values from VTA
	*/

	@JsonProperty(value = "Xid")
	@Size(max = 50, message = "AuthReqCardInfo - Xid, length cannot be more than 50 character")
	String xid;

	@JsonProperty(value = "CCIN")
	@Size(max = 6, message = "AuthReqCardInfo - CCIN, length cannot be more than 6 character")
	String ccin;

	/*	@JsonProperty(value = "CardIdSignature")
		String cardIdSignature;// TODO: leaving without validation for now, but will eventually want length validation
	
	@JsonProperty(value = "CardType")
		@Size(max = 10, message = "AuthReqCardInfo - CardType, length cannot be more than 10 character")
		String cardType;
	
	@JsonProperty(value = "Cavv")
		@Size(max = 56, message = "AuthReqCardInfo->Cavv, length cannot be more than 56 characters")
		String cavv;
	
	@JsonProperty(value = "Cryptogram")
	@Size(max = 64, message = "AuthReqCardInfo - Cryptogram, length cannot be more than 64 character")
	String cryptogram;
	
	@JsonProperty(value = "ECIData")
		@Size(max = 64, message = "AuthReqCardInfo - ECIData, length cannot be more than 64 character")
		String eciData;*/

	@JsonProperty(value = "ExpirationDate")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MMyyyy") // this can also accept an integer array of [year,month]. See the jackson class YearMonthDeserializer
	@Schema(type = "string", format = "yearmonth", example = "072020",
			description = "This should either be a String of pattern MMyyyy or it should be an array of integers where the first integer is the year and the second integer is the month")
	YearMonth expirationDate;

	/*	@JsonProperty(value = "IssuerCountry")
		@Size(max = 3, message = "AuthReqCardInfo - IssuerCountry, length cannot be more than 3 character") // TODO: Pratima thought it was this value but was unsure
		String issuerCountry;
	
	@JsonProperty(value = "PORequired")
		YNFlag poRequired;// TODO: We don't seem to be saving this info
	*/
	@JsonProperty(value = "PaymentType")
	@NotNull(message = "PaymentType must not be null!")
	PaymentMethod paymentType;

	/*@JsonProperty(value = "PinRequired")
	YNFlag pinRequired;// TODO: We don't seem to be saving this info
	
		@JsonProperty(value = "PrePaidCard")
		YNFlag prePaidCard;// TODO: We don't seem to be saving this info
		//TODO: We are getting N/P from VTA . That is most likely correct from cardmain/dpsws code
	
	@JsonProperty(value = "PurchasingCard")
		@Size(max = 1, message = "AuthReqCardInfo - PurchasingCard, length cannot be more than 1 character")
		String purchasingCard; // TODO: Is this Y,N or 1,2,3? //From card service, need to find example
	
		@JsonProperty(value = "StaplesPaymentType")
		@Size(max = 4, message = "AuthReqCardInfo - StaplesPaymentType, length cannot be more than 4 character")
		String staplesPaymentType;*/
}
