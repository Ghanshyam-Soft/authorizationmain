package com.staples.payment.authorization.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.CreateTokenRequest;
import com.staples.payment.authorization.response.AuthResponse;
import com.staples.payment.authorization.response.CreateTokenResponse;
import com.staples.payment.authorization.service.CreateTokenService;
import com.staples.payment.authorization.service.PaymentProcessingService;
import com.staples.payment.authorization.util.EncryptionUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping(value = "/gpas", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "Authorization") // Tells the swagger ui to send the auth token with the requests to these endpoints when using the "Try it out" functionality
public class GpasController
{
	private static final Logger audit = LoggerFactory.getLogger("audit-log");

	private final PaymentProcessingService paymentProcessingService;
	private final EncryptionUtil encryptionUtil;
	private final CreateTokenService createTokenService;

	public GpasController(PaymentProcessingService paymentProcessingService, EncryptionUtil encryptionUtil, CreateTokenService createTokenService)
	{
		this.paymentProcessingService = paymentProcessingService;
		this.encryptionUtil = encryptionUtil;
		this.createTokenService = createTokenService;
	}

	@PostMapping("/auth/v01")
	@Operation(method = "POST",
			description = "Consumers can call this endpoint for pre-auth, auth, and related functionality",
			summary = "GPAS Auth request api")
	@ApiResponses(value = { // TODO: Add other error types. Unprocessable entity
			@ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content), // TODO: We are not currently sending back these errors but we really should be.
			@ApiResponse(responseCode = "504", description = "Gateway Timeout", content = @Content),
	})
	public AuthResponse authEndpoint(@Parameter(name = "payment request body", description = "payment request payload", required = true) @RequestBody @Valid final AuthRequest request)
	// TODO: Need to ensure the request is being printed to the log if they send us a complete invalid request (missing brackets, invalid enum, etc.)
	{
		final Instant receivedTime = Instant.now();

		final String childGUID = request.getTransactionHeader().getChildGUID();
		audit.info("payment service started  {} for ChildGUID: {} ", receivedTime, childGUID);

		log.info("*****************************************************************************************************************************");
		log.info("payment service started at {} for initial request {}", receivedTime, request);

		String auditStatus = null;
		try
		{
			final AuthResponse bankAuthResponse = paymentProcessingService.processAuthRequest(request, receivedTime);

			log.info("Response for childguid {} : {} ", childGUID, bankAuthResponse);
			auditStatus = "Success";

			return bankAuthResponse;
		}
		/*TODO: Pratima discussed wanting actual messages in error responese in prod. 
		
		Ideas:
		-Could use ResponseStatusExceptions
		-Could use the reason field of @ResponseStatus in each Exception
		-Ensure that the custom exceptions we are throwing have good messages/actual exception types and suppress the messages of the rest
		
		Likely best of all, could use some combination of the above.
		*/
		catch(Exception e)
		{
			final String encryptedMessage = encryptionUtil.encryptMessageForLog(request);
			log.error("Auth endpoint exception : : guid {} : input message {} ", childGUID, encryptedMessage, e);
			auditStatus = "Failed with exception, check error log";

			throw e;
		}
		finally
		{
			if(StringUtils.hasText(childGUID))
			{
				paymentProcessingService.updateAuthLogStatusForError(childGUID); // TODO: Is this slowing this down. Is it necessary? Leave for currently release. Need to ensure the issue this solves can't happen anymore.
			}

			final long timeTaken = ChronoUnit.MILLIS.between(receivedTime, Instant.now());
			audit.info("payment service ended for ChildGUID = {}, time taken = {} ms, status = {} ", childGUID, timeTaken, auditStatus);
		}
	}

	@PostMapping("/token/v01")
	@Operation(method = "POST",
			description = "Consumers can call this endpoint for PayPal client token",
			summary = "GPAS Token API")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Success"),
			@ApiResponse(responseCode = "400", description = "Bad Request", content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content),
			@ApiResponse(responseCode = "502", description = "Bad Gateway", content = @Content),
			@ApiResponse(responseCode = "504", description = "Gateway Timeout", content = @Content),
	})
	public CreateTokenResponse clientToken(@Parameter(name = "Token request body", description = "Token request payload", required = true) @RequestBody @Valid final CreateTokenRequest request)
	{
		final String internalRequestId = UUID.randomUUID().toString();

		final Instant startTime = Instant.now();
		audit.info("payment service token call started for internalRequestId {} .", internalRequestId);

		String auditStatus = null;
		try
		{
			CreateTokenResponse tokenResponse = createTokenService.processCreateTokenRequest(request, internalRequestId);

			auditStatus = "Success";

			return tokenResponse;
		}
		catch(Exception e)
		{
			final String encryptedMessage = encryptionUtil.encryptMessageForLog(request);
			log.error("Token endpoint exception :: internalRequestId {} , input message {} ", internalRequestId, encryptedMessage, e);
			auditStatus = "Failed with exception, check error log";

			throw e;
		}
		finally
		{
			final long timeTaken = ChronoUnit.MILLIS.between(startTime, Instant.now());
			audit.info("payment service token call ended for internalRequestId {} , time taken = {} ms, status = {} ", internalRequestId, timeTaken, auditStatus);
		}
	}
}