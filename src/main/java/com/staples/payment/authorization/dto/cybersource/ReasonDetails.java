package com.staples.payment.authorization.dto.cybersource;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
public class ReasonDetails
{
	String code;
	String description;
}
