package com.staples.payment.authorization.dto.cardConsent;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
public class CardConsentKey
{
	String customerId;
	String businessUnit;
	String division;
	String paymentSeq;
	String an;
}
