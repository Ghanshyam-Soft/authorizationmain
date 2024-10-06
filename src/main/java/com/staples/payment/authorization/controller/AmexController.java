package com.staples.payment.authorization.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.staples.payment.authorization.bank.response.AmexPWPBalanceResponse;
import com.staples.payment.authorization.service.bank.AmexService;
import com.staples.payment.shared.amexpwp.request.GetBalanceRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping(value = "/gpas/rewards", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "Authorization") // Tells the swagger ui to send the auth token with the requests to these endpoints when using the "Try it out" functionality
public class AmexController
{

	private static final Logger audit = LoggerFactory.getLogger("audit-log");

	private final AmexService amexService;

	public AmexController(AmexService amexService)
	{
		this.amexService = amexService;
	}

	@PostMapping("/getbalance")
	@Operation(method = "POST",
			description = "Consumers can call this endpoint for Amex rewards balance enquiry",
			summary = "Amex Get Balance API")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content),
	})
	public AmexPWPBalanceResponse amexGetBalance(
			@Parameter(name = "Amex get balance", description = "AmexGetBalance request payload", required = true) @RequestBody @Valid final GetBalanceRequest request)
	{
		final String messageId = request.getMessageId();

		final Instant startTime = Instant.now();
		audit.info("payment service AmexPwp started for messageId {} .", messageId);

		String auditStatus = null;
		try
		{
			AmexPWPBalanceResponse response = amexService.getBalance(request);
			auditStatus = "Success";

			return response;
		}
		catch(Exception e)
		{
			log.error("AmexPWP endpoint exception :: messageId {} ", messageId, e);
			auditStatus = "Failed with exception, check error log";
			throw e;
		}
		finally
		{
			final long timeTaken = ChronoUnit.MILLIS.between(startTime, Instant.now());
			audit.info("payment service AmexPwp ended for messageId {} , time taken = {} ms, status = {} ", messageId, timeTaken, auditStatus);
		}
	}
}