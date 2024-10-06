package com.staples.payment.authorization.bank.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.staples.payment.shared.constant.GpasRespCode;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class AmexResponseStatus
{
	@JsonProperty("ResponseCode")
	GpasRespCode responseCode;

	@JsonProperty("ReasonCode")
	String reasonCode;

	@JsonProperty("VendorResponseCode")
	String vendorResponseCode;

	@JsonProperty("VendorReasonCode")
	String vendorReasonCode;

	@JsonProperty("DescriptionText")
	String descriptionText;

	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonProperty("VendorDescription")
	String vendorDescription;
}
