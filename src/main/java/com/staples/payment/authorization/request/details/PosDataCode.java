package com.staples.payment.authorization.request.details;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.staples.payment.shared.constant.POSDataConstants.AuthorizationMode;
import com.staples.payment.shared.constant.POSDataConstants.CardCaptureSource;
import com.staples.payment.shared.constant.POSDataConstants.CardPresent;
import com.staples.payment.shared.constant.POSDataConstants.InputMethod;
import com.staples.payment.shared.constant.POSDataConstants.PinCaptureCapable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "RequestPOSDataCode")
public class PosDataCode
{
	@JsonProperty(value = "CardCaptureSource")
	@NotNull
	CardCaptureSource cardCaptureSource;

	@JsonProperty(value = "PinCaptureCapable")
	@NotNull
	PinCaptureCapable pinCaptureCapable;

	@JsonProperty(value = "InputMethod")
	@NotNull
	InputMethod inputMethod;

	@JsonProperty(value = "CardPresent")
	@NotNull
	CardPresent cardPresent;

	@JsonProperty(value = "AuthorizationMode")
	AuthorizationMode authorizationMode;

	public String toCodeString()
	{
		String codeString = "" + cardCaptureSource + pinCaptureCapable + inputMethod + cardPresent;

		if(authorizationMode != null)
		{
			codeString = codeString + authorizationMode;
		}

		return codeString;
	}
}
