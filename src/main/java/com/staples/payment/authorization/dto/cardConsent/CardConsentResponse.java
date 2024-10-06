package com.staples.payment.authorization.dto.cardConsent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardConsentResponse
{
	CardConsentKey cardConsentKey;
	String clientId;
	String function;
	String customerFirstName;
	String customerLastName;
	String cardType;
	String cardExpirationDate;
	Instant optInDatetime;
	Instant optOutDatetime;
	String initiatedBy;
	String billTo;
	String shipTo;
	String paymentFrequency;
	BigDecimal amount; // Represented on CCS side as a Double but using BigDecimal here to ensure no loss of precision
	String userID;
	String status; // See CardConsentStatus constant class
	LocalDateTime created_at;
	LocalDateTime updated_at;
	String initialTransactionId;
	String initialTransactionDate;
}
