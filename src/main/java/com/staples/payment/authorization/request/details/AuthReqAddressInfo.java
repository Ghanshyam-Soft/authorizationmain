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
@Schema(name = "RequestAddressInfo")
public class AuthReqAddressInfo
{
	@JsonProperty(value = "IPAddress")
	@Size(max = 45, message = "AuthReqAddressInfo->IPAddress, length cannot be more than 45 character!")
	String ipAddress; // 45 characters is the max length of a non-link-local IPv6 address. We should not be getting link local addresses.

	@JsonProperty(value = "BillToEmail")
	@Email
	@Size(max = 128, message = "AuthReqAddressInfo->Email, length cannot be more than 128 character!")
	@Schema(description = "If present, must either be null or a valid email address.")
	String billToEmail;

	@JsonProperty(value = "BillToCompanyName")
	@Size(max = 50, message = "AuthReqAddressInfo->BillToCompanyName, length cannot be more than 50 character!")
	String billToCompanyName;

	@JsonProperty(value = "BillToFirstName")
	@Size(max = 40, message = "AuthReqAddressInfo->BillToFirstName, length cannot be more than 40 character!")
	String billToFirstName;

	@JsonProperty(value = "BillToLastName")
	@Size(max = 40, message = "AuthReqAddressInfo->BillToLastName, length cannot be more than 40 character!")
	String billToLastName;

	@JsonProperty(value = "BillToAddress1")
	@Size(max = 70, message = "AuthReqAddressInfo->BillToAddress1, length cannot be more than 70 character!")
	String billToAddress1;

	@JsonProperty(value = "BillToAddress2")
	@Size(max = 70, message = "AuthReqAddressInfo->BillToAddress2, length cannot be more than 70 character!")
	String billToAddress2;

	@JsonProperty(value = "BillToCity")
	@Size(max = 35, message = "AuthReqAddressInfo->BillToCity, length cannot be more than 35 character!")
	String billToCity;

	@JsonProperty(value = "BillToState")
	@Size(max = 2, message = "AuthReqAddressInfo->billToState, length cannot be more than 2 character!")
	String billToState;

	@JsonProperty(value = "BillToZipCode")
	@Size(max = 9, message = "AuthReqAddressInfo->billToZipcode, length cannot be more than 9 character!")
	String billToZipCode;

	@JsonProperty(value = "BillToPhoneNumber")
	@Size(max = 15, message = "AuthReqAddressInfo->BillToPhoneNumber, length cannot be more than 15 character!")
	String billToPhoneNumber;

	@JsonProperty(value = "BillToCountry")
	@Size(max = 2, message = "AuthReqAddressInfo->BillToCountry, length cannot be more than 2 character!")
	String billToCountry;

	@JsonProperty(value = "ShipToFirstName")
	@Size(max = 40, message = "AuthReqAddressInfo->ShipToFirstName, length cannot be more than 40 character!")
	String shipToFirstName;

	@JsonProperty(value = "ShipToLastName")
	@Size(max = 40, message = "AuthReqAddressInfo->ShipToLastName, length cannot be more than 40 character!")
	String shipToLastName;

	@JsonProperty(value = "ShipToAddress1")
	@Size(max = 70, message = "AuthReqAddressInfo->ShipToAddress1, length cannot be more than 70 character!")
	String shipToAddress1;

	@JsonProperty(value = "ShipToAddress2")
	@Size(max = 70, message = "AuthReqAddressInfo->ShipToAddress2, length cannot be more than 70 character!")
	String shipToAddress2;

	@JsonProperty(value = "ShipToCity")
	@Size(max = 35, message = "AuthReqAddressInfo->ShipToCity, length cannot be more than 35 character!")
	String shipToCity;

	@JsonProperty(value = "ShipToState")
	@Size(max = 2, message = "AuthReqAddressInfo->ShipToState, length cannot be more than 2 character!")
	String shipToState;

	@JsonProperty(value = "ShipToZipCode")
	@Size(max = 9, message = "AuthReqAddressInfo->ShipToZipCode, length cannot be more than 9 character!")
	String shipToZipCode;

	@JsonProperty(value = "ShipToCountry")
	@Size(max = 2, message = "AuthReqAddressInfo->ShipToCountry, length cannot be more than 2 character!")
	String shipToCountry;

	@JsonProperty(value = "ShipToPhoneNumber")
	@Size(max = 15, message = "AuthReqAddressInfo->ShipToPhoneNumber, length cannot be more than 15 character!")
	String shipToPhoneNumber;
}