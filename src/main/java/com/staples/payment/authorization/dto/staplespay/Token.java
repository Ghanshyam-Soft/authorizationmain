package com.staples.payment.authorization.dto.staplespay;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Token
{
	String tokenType;
	String value;
	Long validUntil;
}