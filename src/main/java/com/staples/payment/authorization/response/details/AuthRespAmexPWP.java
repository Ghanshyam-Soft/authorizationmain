package com.staples.payment.authorization.response.details;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.staples.payment.shared.constant.GpasRespCode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({
		"pwpReservationInfo",
		"pwpBalanceInfo",
		"pwpResponseCode",
		"pwpResponseDesc"
})
@Schema(name = "ResponseAmexPWP")
public class AuthRespAmexPWP
{
	@JsonProperty(value = "PWPReservationInfo", required = true)
	AuthRespPWPReservationInfo pwpReservationInfo;

	@JsonProperty(value = "PWPBalanceInfo", required = true)
	AuthRespBalanceInfo pwpBalanceInfo;

	@JsonProperty(value = "PWPResponseCode", required = true)
	GpasRespCode pwpResponseCode;

	@JsonProperty(value = "PWPResponseDesc", required = true)
	String pwpResponseDesc;
}