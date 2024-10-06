package com.staples.payment.authorization.bank.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class AmexPWPAccountKey
{
	@JsonProperty("AccountId")
	String accountId;
}
