package com.staples.payment.authorization.service.bank.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.staples.payment.authorization.clients.BraintreeAuthClient;
import com.staples.payment.authorization.exception.BankCommTimeoutException;
import com.staples.payment.authorization.exception.BankResponseInvalidException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.details.AuthReqPayPalInfo;
import com.staples.payment.authorization.service.bank.BraintreePaymentService;
import com.staples.payment.authorization.service.factory.BraintreeRequestFactory;
import com.staples.payment.shared.braintree.request.SaleRequest;
import com.staples.payment.shared.braintree.request.VoidRequest;
import com.staples.payment.shared.braintree.response.TransactionResponse;
import com.staples.payment.shared.cache.ResponseInfoCache;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.Bank;
import com.staples.payment.shared.constant.GpasRespCode;
import com.staples.payment.shared.constant.MessageStatus;
import com.staples.payment.shared.constant.PaymentType;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.braintree.BraintreeResponse;
import com.staples.payment.shared.entity.respInfo.ReasonResponseInfo;
import com.staples.payment.shared.entity.respInfo.RespResponseInfo;
import com.staples.payment.shared.repo.AuthLogRepo;
import com.staples.payment.shared.repo.bank.BraintreeResponseRepo;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BraintreePaymentServiceImpl implements BraintreePaymentService
{
	private static final Logger audit = LoggerFactory.getLogger("audit-log");

	private final BraintreeAuthClient braintreeAuthClient;
	private final ResponseInfoCache responseInfoCache;
	private final AuthLogRepo authLogRepo;
	private final BraintreeResponseRepo braintreeResponseRepo;
	private final BraintreeRequestFactory braintreeRequestFactory;
	private final ObjectMapper objectMapper;

	private final Bank bankId = Bank.BRAINTREE;

	public BraintreePaymentServiceImpl(BraintreeAuthClient braintreeAuthClient, ResponseInfoCache responseInfoCache, AuthLogRepo authLogRepo, BraintreeResponseRepo braintreeResponseRepo,
			BraintreeRequestFactory braintreeRequestFactory, ObjectMapper objectMapper)
	{
		super();
		this.braintreeAuthClient = braintreeAuthClient;
		this.responseInfoCache = responseInfoCache;
		this.authLogRepo = authLogRepo;
		this.braintreeResponseRepo = braintreeResponseRepo;
		this.braintreeRequestFactory = braintreeRequestFactory;
		this.objectMapper = objectMapper;
	}

	@Override
	public AuthLog process(AuthLog authLog, AuthRequest gpasRequest)
	{
		try
		{
			PaymentType paymentType = gpasRequest.getTransactionHeader().getPaymentType();
			if(paymentType == PaymentType.PayPal)
			{
				authLog = processPaypalRequest(authLog, gpasRequest);
			}
			else
			{
				throw new RuntimeException("invalid payment type for BRAINTREE");
			}

			return authLog;
		}
		catch(final BankCommTimeoutException e) // TODO: Make sure this is thrown for timeout
		{
			updateAuthLogForError(authLog, MessageStatus.Timeout);
			throw e;
		}
		catch(final Exception e)
		{
			updateAuthLogForError(authLog, MessageStatus.System_Issue);
			throw e;
		}
	}

	private AuthLog processPaypalRequest(AuthLog authLog, AuthRequest gpasRequest)
	{
		AuthRequestType requestType = gpasRequest.getTransactionHeader().getRequestType();

		if(requestType == AuthRequestType.Authorization || requestType == AuthRequestType.ReAuthorization)
		{
			TransactionResponse saleResponse = sale(gpasRequest, authLog);

			authLog = handleBraintreeResponse(authLog, saleResponse, gpasRequest);
		}
		else if(requestType == AuthRequestType.Reversal)
		{
			authLog = processReversalRequest(authLog, gpasRequest);
		}
		else
		{
			throw new RuntimeException("invalid request type for PAYPAL");
		}

		return authLog;
	}

	private AuthLog processReversalRequest(AuthLog authLog, AuthRequest gpasRequest)
	{
		val transactionHeader = gpasRequest.getTransactionHeader();
		val reversalGuid = transactionHeader.getReversalGUID();
		val childGuid = transactionHeader.getChildGUID();

		Optional<AuthLog> originalAuthLogOptional = authLogRepo.findById(reversalGuid);

		if(originalAuthLogOptional.isPresent())
		{
			AuthLog originalAuthLog = originalAuthLogOptional.get();

			BraintreeResponse braintreeResponse = originalAuthLog.getBraintreeResponse();

			if(braintreeResponse != null)
			{
				TransactionResponse reversalResponse = voidTransaction(gpasRequest, braintreeResponse);
				authLog = handleBraintreeResponse(authLog, reversalResponse, gpasRequest);
			}
			else
			{
				throw new RuntimeException(
						"no braintree response for the original transaction for which the Reversal is tried, with gpasKey : " + originalAuthLog.getGpasKey() + " and childGuid : " + childGuid);
			}
		}
		else
		{
			throw new RuntimeException("original transaction is not present in database for the Reversal, where reversalGuid : " + reversalGuid + " and childGuid : " + childGuid);
		}

		return authLog;
	}

	private TransactionResponse sale(AuthRequest gpasRequest, AuthLog authLog)
	{
		SaleRequest request = braintreeRequestFactory.createSaleRequest(gpasRequest, authLog);
		val startTime = Instant.now();
		val childGuid = gpasRequest.getTransactionHeader().getChildGUID();
		val requestType = gpasRequest.getTransactionHeader().getRequestType();
		audit.info("Child GUID = {} with RequestType = {}, started calling Braintee bank service at {} ", childGuid, requestType, startTime);

		val response = braintreeAuthClient.sale(request);

		Instant endTime = Instant.now();
		audit.info("Child GUID = {} with Response Code = {}, finished calling Braintee bank service at {} ", childGuid, response.getProcessorResponseCode(), endTime);
		audit.info("Childguid: " + childGuid + ", Time taken for Braintree bank call: " + ChronoUnit.MILLIS.between(startTime, endTime) + " ms");

		return response;
	}

	private TransactionResponse voidTransaction(AuthRequest gpasRequest, BraintreeResponse braintreeResponse)
	{
		VoidRequest request = braintreeRequestFactory.createVoidRequest(braintreeResponse);
		val startTime = Instant.now();
		val childGuid = gpasRequest.getTransactionHeader().getChildGUID();
		val requestType = gpasRequest.getTransactionHeader().getRequestType();
		audit.info("Child GUID = {} with RequestType = {}, started calling Braintee bank service at {} ", childGuid, requestType, startTime);

		val response = braintreeAuthClient.voidTransaction(request);

		Instant endTime = Instant.now();
		audit.info("Child GUID = {} with Response Code = {}, finished calling Braintee bank service at {} ", childGuid, response.getProcessorResponseCode(), endTime);
		audit.info("Childguid: " + childGuid + ", Time taken for Braintree bank call: " + ChronoUnit.MILLIS.between(startTime, endTime) + " ms");

		return response;
	}

	private AuthLog handleBraintreeResponse(AuthLog authLog, TransactionResponse response, AuthRequest gpasRequest)
	{
		BraintreeResponse responseEntity = saveBraintreeResponse(response, authLog.getGpasKey(), gpasRequest);
		authLog = updateAuthLogWithBraintreeResponse(response, authLog, responseEntity);

		return authLog;
	}

	private BraintreeResponse saveBraintreeResponse(TransactionResponse response, String gpasKey, AuthRequest gpasRequest)
	{
		try
		{
			AuthReqPayPalInfo reqPayPalInfo = gpasRequest.getPayPalInfo();

			// Made so these failing doesn't bring everything down
			final String transactionStatusHistory = convertToString(response.getTransactionStatusHistory(), gpasKey);
			final String validationError = convertToString(response.getValidationError(), gpasKey);
			final String deepValidationError = convertToString(response.getDeepValidationError(), gpasKey);
			final String customFields = convertToString(response.getCustomFields(), gpasKey);

			BraintreeResponse braintreeResponse = BraintreeResponse.builder() // TODO: Store message field (and any other missing field)
					.gpasKey(gpasKey)
					.transactionId(response.getTransactionId())
					.transactionType(response.getTransactionType())
					.transactionStatus(response.getTransactionStatus())
					.paymentInstrumentType(response.getPaymentInstrumentType())
					.captureId(response.getCaptureId())
					.payerId(response.getPayerId())
					.payerEmail(response.getPayerEmail())
					.payerPhone(response.getPayerPhone())
					.payerStatus(response.getPayerStatus())
					.sellerProtectionStatus(response.getSellerProtectionStatus())
					.paymentMethodNonce(response.getPaymentMethodNonce())
					.additionalProcessorResponse(response.getAdditionalProcessorResponse())
					.cvvResponseCode(response.getCvvResponseCode())
					.graphQlId(response.getGraphQLId())
					.networkResponseCode(response.getNetworkResponseCode())
					.networkResponseText(response.getNetworkResponseText())
					.networkTransactionId(response.getNetworkTransactionId())
					.retrievalReferenceNumber(response.getRetrievalReferenceNumber())
					.transactionStatusHistory(transactionStatusHistory)
					.validationError(validationError)
					.deepValidationError(deepValidationError)
					.gatewayRejectionReason(response.getGatewayRejectionReason())
					.debugId(response.getDebugId())
					.customFields(customFields)
					.authorizationExpiresAt(response.getAuthorizationExpiresAt())
					.trxnCreatedAt(response.getCreatedAt())
					.trxnUpdatedAt(response.getUpdatedAt())
					.dispute(null) // TODO: Add after MVP
					.deviceData(reqPayPalInfo.getDeviceData())
					.message(response.getMessage())
					.build();

			braintreeResponseRepo.insert(braintreeResponse);

			return braintreeResponse;
		}
		catch(Exception ex)
		{
			log.error("Saving braintree_response details failed. {}", ex);
			throw ex; // TODO: Surround with separate exception rather than writing an error log
		}
	}

	private @Nullable String convertToString(@Nullable Object value, String gpasKey)
	{
		try
		{
			final String string = objectMapper.writeValueAsString(value);

			return string;
		}
		catch(JsonProcessingException e)
		{
			log.error("Failed conversion of an inner json in Braintree Response to string for gpasKey {}", gpasKey);
			return null;
		}
	}

	private AuthLog updateAuthLogWithBraintreeResponse(TransactionResponse saleResponse, AuthLog authLog, BraintreeResponse responseEntity)
	{
		RespResponseInfo responseInfoResp = responseInfoCache.getRespResponseInfoBy(bankId, saleResponse.getProcessorResponseCode(), null, saleResponse.getProcessorResponseText());
		ReasonResponseInfo responseInfoReas = responseInfoCache.getReasonResponseInfoBy(bankId, saleResponse.getProcessorResponseCode(), null, saleResponse.getProcessorResponseText());

		authLog.setRespRcvdFromBankDatetime(null); // TODO: time received
		authLog.setRequestSentToBankDatetime(null); // TODO: time sent

		authLog.setGpasReasCode(responseInfoReas.getGpasCode());
		authLog.setGpasRespCode(responseInfoResp.getGpasCode());

		authLog.setVendorCvvCode(saleResponse.getCvvResponseCode()); // TODO: When we use apple pay, will need to use ResponseInfo conversion on this
		authLog.setVendorReasCode(responseInfoReas.getBankCode());
		authLog.setVendorRespCode(responseInfoResp.getBankCode());

		authLog.setAuthCode(saleResponse.getAuthorizationId());

		final String gpasResponseDesc = responseInfoResp.getGpasCodeDesc() + " " + responseInfoReas.getGpasCodeDesc();
		authLog.setGpasResponseDescription(gpasResponseDesc);

		final BigDecimal approvedAmount;
		if(GpasRespCode.A == responseInfoResp.getGpasCode())
		{
			approvedAmount = saleResponse.getAmount();
		}
		else
		{
			approvedAmount = new BigDecimal("0.00");
		}

		authLog.setApprovedAmount(approvedAmount);

		if(authLog.getPaymentToken() == null)
		{
			authLog.setPaymentToken(saleResponse.getPaymentMethodToken());
		}
		else if(authLog.getPaymentToken() != null && saleResponse.getPaymentMethodToken() != null && !authLog.getPaymentToken().equals(saleResponse.getPaymentMethodToken()))
		{
			log.error("Long term payment token received in the request and response from Braintree has a mismatch");
			throw new BankResponseInvalidException("Long term payment token mismatch");
		}

		authLog.setMessageStatus(MessageStatus.Successful);
		authLog.setBraintreeResponse(responseEntity); // Setting here doesn't effect db, rather it ensures that the object in memory matches the db

		try
		{
			authLogRepo.update(authLog);
		}
		catch(Exception e)
		{
			log.error("Updating authLog with Braintree response failed. {}", authLog);
			throw e;
		}

		return authLog;
	}

	private void updateAuthLogForError(AuthLog authLog, MessageStatus messageStatus)
	{
		authLog.setMessageStatus(messageStatus);
		try
		{
			authLogRepo.update(authLog);
		}
		catch(Exception e)
		{
			log.error("Updating authLog failed. {}", authLog);
			throw e;
		}
	}
}