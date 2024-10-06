package com.staples.payment.authorization.response.details;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(name = "ResponseCOFInfo")
public class AuthRespCOFInfo // Card On File
{
	String initialTransactionId;
	String initialTransactionDate;
}