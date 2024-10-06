package com.staples.payment.authorization.request.details;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.staples.payment.shared.constant.cof.BillingType;
import com.staples.payment.shared.constant.cof.COFSchedInd;
import com.staples.payment.shared.constant.cof.StoredCredInd;
import com.staples.payment.shared.constant.cof.TransactionInitiator;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "RequestCOFInfo")
public class AuthReqCOFInfo // Card On File
{
	@JsonProperty(value = "TranInit")
	TransactionInitiator transactionInitiator;

	@JsonProperty(value = "StoredCredInd")
	StoredCredInd storedCredInd;

	@JsonProperty(value = "COFSchedInd")
	COFSchedInd cofSchedInd;

	@JsonProperty(value = "BillingType")
	BillingType billingType;
}