package com.staples.payment.authorization.dto.staplespay;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
public class StPayAuthToken
{
	Token token;
}
