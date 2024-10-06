package com.staples.payment.authorization.response.details;

import java.math.BigDecimal;
import java.time.Instant;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.staples.payment.shared.constant.AvsResponseCode;
import com.staples.payment.shared.constant.CcinResponseCode;
import com.staples.payment.shared.constant.GpasRespCode;
import com.staples.payment.shared.constant.MessageStatus;
import com.staples.payment.shared.constant.PaymentMethod;

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
		"methodProcessed",
		"authorizationCode",
		"responseCode",
		"reasonCode",
		"ccinResponseCode",
		"avsResponseCode",
		"vendorResponseCode",
		"vendorReasonCode",
		"vendorCCINResponseCode",
		"vendorAVSResponseCode",
		"poRequiredResponseCode",
		"amountApproved",
		"amountRequested",
		"remainingBalance",
		"descriptionText",
		"vendorInfoBlock",
		"messageStatus",
		"authExpiryDate",
		"cavvResultCode",
		"authorizationExpiresAt",
		"paymentToken"
})
@Schema(name = "ResponseTransactionDetail")
public class AuthRespTransactionDetail
{
	@JsonProperty(value = "MethodProcessed", required = true)
	@Schema(required = true)
	PaymentMethod methodProcessed;

	@JsonProperty(value = "AuthorizationCode", required = true)
	@Schema(required = true)
	String authorizationCode;

	@JsonProperty(value = "ResponseCode", required = true)
	@Schema(required = true)
	GpasRespCode responseCode;

	@JsonProperty(value = "ReasonCode", required = true)
	@Schema(required = true, description = "The various values span a range from 00 to 99, with all being strings made of two digits.")
	String reasonCode;

	@JsonProperty(value = "CCINResponseCode", required = true)
	@Schema(required = true)
	CcinResponseCode ccinResponseCode;

	@JsonProperty(value = "AVSResponseCode", required = true)
	@Schema(required = true)
	AvsResponseCode avsResponseCode;

	@JsonProperty(value = "VendorResponseCode")
	String vendorResponseCode;

	@JsonProperty(value = "VendorReasonCode")
	String vendorReasonCode;

	@JsonProperty(value = "VendorCCINResponseCode")
	String vendorCCINResponseCode;

	@JsonProperty(value = "VendorAVSResponseCode")
	String vendorAVSResponseCode;

	@JsonProperty(value = "PORequiredResponseCode", required = true)
	@Schema(required = true)
	boolean poRequiredResponseCode;

	@JsonProperty(value = "AmountApproved", required = true)
	@Schema(required = true)
	BigDecimal amountApproved;

	@JsonProperty(value = "AmountRequested", required = true)
	@Schema(required = true)
	BigDecimal amountRequested;

	@JsonProperty(value = "RemainingBalance", required = true)
	@Schema(required = true)
	BigDecimal remainingBalance;

	@JsonProperty(value = "DescriptionText", required = true)
	@Schema(required = true)
	String descriptionText;

	@JsonProperty(value = "VendorInfoBlock", required = true)
	@Schema(required = true)
	String vendorInfoBlock;

	@JsonProperty(value = "MessageStatus", required = true)
	@Schema(required = true)
	MessageStatus messageStatus;

	/*	@JsonProperty(value = "CheckType", required = true)
		@Schema(required = true)
		String checkType;
	*/
	@JsonProperty(value = "CAVVResultCode")
	String cavvResultCode;

	@JsonProperty(value = "Xid")
	String xid;

	@JsonProperty(value = "CAVV")
	String cavv;

	@JsonProperty(value = "EciFlag")
	String eciFlag;

	@JsonProperty(value = "AuthorizationExpiresAt")
	@Size(max = 20)
	Instant authorizationExpiresAt;

	@JsonProperty(value = "PaymentToken")
	@Size(max = 64, message = "AuthRespTransactionDetail->PaymentToken, length cannot be more than 64 character")
	String paymentToken;
}