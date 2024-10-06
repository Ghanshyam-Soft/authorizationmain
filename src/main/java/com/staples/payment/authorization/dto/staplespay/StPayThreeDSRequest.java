package com.staples.payment.authorization.dto.staplespay;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StPayThreeDSRequest
{
	String merchantID;
	String merchantReferenceID;
	String threeDSServerTransID;
}
