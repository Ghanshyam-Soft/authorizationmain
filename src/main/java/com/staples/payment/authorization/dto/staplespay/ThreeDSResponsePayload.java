package com.staples.payment.authorization.dto.staplespay;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
public class ThreeDSResponsePayload
{
	String errorComponent;
	String acsTransID;
	String encodedCReq;
	String errorCode;
	String eci;
	String acsURL;
	String dsReferenceNumber;
	String acsReferenceNumber;
	String messageType;
	String dsTransID;
	String acsOperatorID;
	String errorDetail;
	String messageVersion;
	String authenticationType;
	String acsChallengeMandated;
	String transStatus;
	String threeDSServerTransID;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	LocalDate createDate;
}
