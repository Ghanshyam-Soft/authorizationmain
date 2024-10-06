package com.staples.payment.authorization.service.bank.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.staples.payment.authorization.clients.AciAuthClient;
import com.staples.payment.authorization.configuration.properties.EnabledFeatures;
import com.staples.payment.authorization.exception.BankCommTimeoutException;
import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.details.AuthReqTransactionHeader;
import com.staples.payment.authorization.service.bank.AciPaymentService;
import com.staples.payment.authorization.service.factory.AciRequestFactory;
import com.staples.payment.shared.aci.constants.pos.EntryMode;
import com.staples.payment.shared.aci.request.AuthorizationCompleteRequest;
import com.staples.payment.shared.aci.request.AuthorizationRequest;
import com.staples.payment.shared.aci.request.BalanceRequest;
import com.staples.payment.shared.aci.request.CardVerificationRequest;
import com.staples.payment.shared.aci.request.PartialReversalRequest;
import com.staples.payment.shared.aci.request.RefundRequest;
import com.staples.payment.shared.aci.request.ReversalRequest;
import com.staples.payment.shared.aci.response.AciBalanceResponse;
import com.staples.payment.shared.aci.response.AciPaymentResponse;
import com.staples.payment.shared.aci.response.ResponseWrapper;
import com.staples.payment.shared.aci.response.detail.CustomParameters;
import com.staples.payment.shared.aci.response.detail.balance.BalanceResult;
import com.staples.payment.shared.aci.response.detail.balance.BalanceResultDetails;
import com.staples.payment.shared.aci.response.detail.balance.Balances;
import com.staples.payment.shared.aci.response.detail.payment.PaymentResult;
import com.staples.payment.shared.aci.response.detail.payment.PaymentResultDetails;
import com.staples.payment.shared.aci.response.detail.payment.ThreeDSecureResult;
import com.staples.payment.shared.cache.ResponseInfoCache;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.AvsResponseCode;
import com.staples.payment.shared.constant.Bank;
import com.staples.payment.shared.constant.GpasRespCode;
import com.staples.payment.shared.constant.MessageStatus;
import com.staples.payment.shared.constant.PaymentMethod;
import com.staples.payment.shared.constant.PaymentType;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.MerchantMaster;
import com.staples.payment.shared.entity.aci.AciAuthResponse;
import com.staples.payment.shared.entity.aci.AciAuthResponse.AciAuthResponseBuilder;
import com.staples.payment.shared.entity.respInfo.AvsResponseInfo;
import com.staples.payment.shared.entity.respInfo.CcinResponseInfo;
import com.staples.payment.shared.entity.respInfo.ReasonResponseInfo;
import com.staples.payment.shared.entity.respInfo.RespResponseInfo;
import com.staples.payment.shared.exceptions.MissingRespInfoException;
import com.staples.payment.shared.repo.AuthLogRepo;
import com.staples.payment.shared.repo.bank.AciAuthResponseRepo;
import com.staples.payment.shared.util.BigDecimalExtensions;

import lombok.val;
import lombok.experimental.ExtensionMethod;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@ExtensionMethod({BigDecimalExtensions.class})
public class AciPaymentServiceImpl implements AciPaymentService
{
	private static final Logger audit = LoggerFactory.getLogger("audit-log");

	private final AciAuthClient aciGatewayClient;
	private final ResponseInfoCache responseInfoCache;
	private final AuthLogRepo authLogRepo;
	private final AciAuthResponseRepo aciResponseRepo;
	private final AciRequestFactory aciRequestFactory;
	private final EnabledFeatures enabledFeatures;

	private final Bank bankId = Bank.ACI;

	public AciPaymentServiceImpl(AciAuthClient aciGatewayClient, ResponseInfoCache responseInfoCache, AuthLogRepo authLogRepo, AciAuthResponseRepo aciResponseRepo,
			AciRequestFactory aciRequestFactory, EnabledFeatures enabledFeatures)
	{
		super();
		this.aciGatewayClient = aciGatewayClient;
		this.responseInfoCache = responseInfoCache;
		this.authLogRepo = authLogRepo;
		this.aciResponseRepo = aciResponseRepo;
		this.aciRequestFactory = aciRequestFactory;
		this.enabledFeatures = enabledFeatures;
	}

	@Override
	public AuthLog process(AuthLog authLog, AuthRequest gpasRequest, MerchantMaster merchantMaster)
	{
		try
		{
			PaymentType paymentType = gpasRequest.getTransactionHeader().getPaymentType();
			if(paymentType == PaymentType.Credit)
			{
				return handleCreditRequest(authLog, gpasRequest, merchantMaster);
			}
			else if(paymentType == PaymentType.GiftCard && enabledFeatures.isGiftcard())
			{
				return handleGiftCardRequest(authLog, gpasRequest, merchantMaster);
			}
			else if(paymentType == PaymentType.Prepaid && enabledFeatures.isPrepaid())
			{
				return handlePrepaidRequest(authLog, gpasRequest, merchantMaster);
			}
			else
			{
				throw new RuntimeException("invalid payment type for ACI");
			}
		}
		catch(final BankCommTimeoutException e) // TODO: Need to make sure this method is thrown
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

	private void updateAuthLogForError(AuthLog authLog, MessageStatus messageStatus)
	{
		authLog.setMessageStatus(messageStatus);

		// TODO: set request and response time if available

		try
		{
			authLogRepo.update(authLog);
		}
		catch(Exception e)
		{
			log.error("Saving authLog failed. {}", authLog);
			throw e;
		}
	}

	private AuthLog handlePrepaidRequest(AuthLog authLog, AuthRequest gpasRequest, MerchantMaster merchantMaster)
	{
		AuthRequestType requestType = gpasRequest.getTransactionHeader().getRequestType();

		if(requestType == AuthRequestType.BalanceInquiry)
		{
			return balanceInquiry(gpasRequest, merchantMaster, authLog);
		}
		else if(List.of(AuthRequestType.Authorization, AuthRequestType.ReAuthorization).contains(requestType))
		{
			return authorization(gpasRequest, merchantMaster, authLog);
		}
		else if(requestType == AuthRequestType.Reversal)
		{
			return reversal(gpasRequest, merchantMaster, authLog);
		}
		else if(requestType == AuthRequestType.PartialReversal)
		{
			return partialReversal(gpasRequest, merchantMaster, authLog);
		}
		else if(requestType == AuthRequestType.Refund)
		{
			return refund(gpasRequest, merchantMaster, authLog);
		}
		else
		{
			throw new InvalidInputException("invalid request type for prepaid card");
		}
	}

	private AuthLog handleGiftCardRequest(AuthLog authLog, AuthRequest gpasRequest, MerchantMaster merchantMaster)
	{
		AuthRequestType requestType = gpasRequest.getTransactionHeader().getRequestType();

		if(requestType == AuthRequestType.BalanceInquiry)
		{
			return balanceInquiry(gpasRequest, merchantMaster, authLog);
		}
		else if(requestType == AuthRequestType.Authorization)
		{
			return authorization(gpasRequest, merchantMaster, authLog);
		}
		else if(requestType == AuthRequestType.AuthorizationComplete)
		{
			return authorizationComplete(gpasRequest, merchantMaster, authLog);
		}
		else if(requestType == AuthRequestType.Reversal)
		{
			return partialReversal(gpasRequest, merchantMaster, authLog); // For GC Reversals, amount is mandatory hence using partialReversal end-point
		}
		else
		{
			throw new InvalidInputException("invalid request type for gift card");
		}
	}

	private AuthLog handleCreditRequest(AuthLog authLog, AuthRequest gpasRequest, MerchantMaster merchantMaster)
	{
		AuthRequestType requestType = gpasRequest.getTransactionHeader().getRequestType();

		if(requestType == AuthRequestType.PreAuthorization)
		{
			return cardVerification(gpasRequest, merchantMaster, authLog);
		}
		else if(List.of(AuthRequestType.Authorization, AuthRequestType.ReAuthorization).contains(requestType))
		{
			return authorization(gpasRequest, merchantMaster, authLog);
		}
		else if(requestType == AuthRequestType.Reversal)
		{
			return reversal(gpasRequest, merchantMaster, authLog);
		}
		else if(requestType == AuthRequestType.PartialReversal)
		{
			return partialReversal(gpasRequest, merchantMaster, authLog);
		}
		else if(requestType == AuthRequestType.Refund)
		{
			return refund(gpasRequest, merchantMaster, authLog);
		}
		else
		{
			throw new InvalidInputException("invalid request type for credit card");
		}
	}

	private AuthLog balanceInquiry(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog)
	{
		BalanceRequest request = aciRequestFactory.createBalanceInquiry(gpasRequest, merchantMaster, authLog);

		val startTime = Instant.now();
		val childGuid = gpasRequest.getTransactionHeader().getChildGUID();
		audit.info("Started calling ACI bank service for balanceInquiry at {}, child guid {}", startTime, childGuid);

		val response = aciGatewayClient.balanceInquiry(request);

		Instant endTime = Instant.now();
		final long duration = ChronoUnit.MILLIS.between(startTime, endTime);
		audit.info("Finished calling ACI bank service for balanceInquiry at {}, duration {} ms, child guid {}", endTime, duration, childGuid);

		authLog = handleBalanceResponse(authLog, response);

		return authLog;
	}

	private AuthLog cardVerification(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog)
	{
		CardVerificationRequest request = aciRequestFactory.createCardVerification(gpasRequest, merchantMaster, authLog);

		val startTime = Instant.now();
		val childGuid = gpasRequest.getTransactionHeader().getChildGUID();
		log.info("Pre-Authorization request to ACI bank service {}, for child guid {}", request, childGuid);
		audit.info("Started calling ACI bank service for cardVerification at {}, child guid {}", startTime, childGuid);

		val response = aciGatewayClient.cardVerification(request);

		Instant endTime = Instant.now();
		final long duration = ChronoUnit.MILLIS.between(startTime, endTime);
		log.info("Pre-Authorization response from ACI bank service {}, for child guid {}", response, childGuid);
		audit.info("Finished calling ACI bank service for cardVerification at {}, duration {} ms, child guid {}", endTime, duration, childGuid);

		authLog = handlePaymentResponse(authLog, response, request.getPos().getEntryMode());

		return authLog;
	}

	private AuthLog authorization(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog)
	{
		AuthorizationRequest request = aciRequestFactory.createAuthorization(gpasRequest, merchantMaster, authLog);

		val startTime = Instant.now();
		val childGuid = gpasRequest.getTransactionHeader().getChildGUID();
		log.info("Authorization request to ACI bank service {}, for child guid {}", request, childGuid);
		audit.info("Started calling ACI bank service for authorization at {}, child guid {}", startTime, childGuid);

		val response = aciGatewayClient.authorization(request);

		Instant endTime = Instant.now();
		final long duration = ChronoUnit.MILLIS.between(startTime, endTime);
		log.info("Authorization response from ACI bank service: {}, for child guid {}", response, childGuid);
		audit.info("Finished calling ACI bank service for authorization at {}, duration {} ms, child guid {}", endTime, duration, childGuid);

		authLog = handlePaymentResponse(authLog, response, request.getPos().getEntryMode());

		return authLog;
	}

	private AuthLog authorizationComplete(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog)
	{
		final AuthReqTransactionHeader transactionHeader = gpasRequest.getTransactionHeader();

		Optional<AuthLog> authLogToCompleteOpt = authLogRepo.findById(transactionHeader.getAuthReferenceGUID());

		if(authLogToCompleteOpt.isPresent())
		{
			AuthLog authLogToComplete = authLogToCompleteOpt.get();

			AuthorizationCompleteRequest request = aciRequestFactory.createAuthorizationComplete(gpasRequest, merchantMaster, authLog, authLogToComplete);

			val startTime = Instant.now();
			val childGuid = gpasRequest.getTransactionHeader().getChildGUID();
			log.info("Authorization Complete request to ACI bank service {}, for child guid {}", request, childGuid);
			audit.info("Started calling ACI bank service for authorization complete at {}, child guid {}", startTime, childGuid);

			val response = aciGatewayClient.authorizationComplete(request);

			Instant endTime = Instant.now();
			final long duration = ChronoUnit.MILLIS.between(startTime, endTime);
			log.info("Authorization Complete response from ACI bank service: {}, for child guid {}", response, childGuid);
			audit.info("Finished calling ACI bank service for authorization complete at {}, duration {} ms, child guid {}", endTime, duration, childGuid);

			authLog = handlePaymentResponse(authLog, response, request.getPos().getEntryMode());

			return authLog;
		}
		else
		{
			throw new InvalidInputException("AuthReferenceGUID for Gift Card completion request did not match any row in the database.");
		}
	}

	private AuthLog refund(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog)
	{
		RefundRequest request = aciRequestFactory.createRefund(gpasRequest, merchantMaster, authLog);

		val startTime = Instant.now();
		val childGuid = gpasRequest.getTransactionHeader().getChildGUID();
		log.info("Refund request to ACI bank service {}, for child guid {}", request, childGuid);
		audit.info("Started calling ACI bank service for refund at {}, child guid {}", startTime, childGuid);

		val response = aciGatewayClient.refund(request);

		Instant endTime = Instant.now();
		final long duration = ChronoUnit.MILLIS.between(startTime, endTime);
		log.info("Pre-Authorization response from ACI bank service {}, for child guid {}", response, childGuid);
		audit.info("Finished calling ACI bank service for refund at {}, duration {} ms, child guid {}", endTime, duration, childGuid);

		authLog = handlePaymentResponse(authLog, response, request.getPos().getEntryMode());

		return authLog;
	}

	private AuthLog reversal(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog)
	{
		final AuthReqTransactionHeader transactionHeader = gpasRequest.getTransactionHeader();

		Optional<AuthLog> authLogToReverseOpt = authLogRepo.findById(transactionHeader.getReversalGUID());
		if(authLogToReverseOpt.isPresent())
		{
			AuthLog authLogToReverse = authLogToReverseOpt.get();

			ReversalRequest request = aciRequestFactory.createReversal(gpasRequest, merchantMaster, authLog, authLogToReverse);

			val startTime = Instant.now();
			val childGuid = gpasRequest.getTransactionHeader().getChildGUID();
			log.info("Reversal request to ACI bank service {}, for child guid {}", request, childGuid);
			audit.info("Started calling ACI bank service for reversal at {}, child guid {}", startTime, childGuid);

			val response = aciGatewayClient.reversal(request);

			Instant endTime = Instant.now();
			final long duration = ChronoUnit.MILLIS.between(startTime, endTime);
			log.info("Reversal response from ACI bank service {}, child guid {}", response, childGuid);
			audit.info("Finished calling ACI bank service for reversal at {}, duration {} ms, child guid {}", endTime, duration, childGuid);

			authLog = handlePaymentResponse(authLog, response, null);

			return authLog;
		}
		else
		{
			throw new InvalidInputException("ReversalGUID did not match any row in the database.");
		}
	}

	private AuthLog partialReversal(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog)
	{
		final AuthReqTransactionHeader transactionHeader = gpasRequest.getTransactionHeader();

		Optional<AuthLog> authLogToReverseOpt = authLogRepo.findById(transactionHeader.getReversalGUID());
		if(authLogToReverseOpt.isPresent())
		{
			AuthLog authLogToReverse = authLogToReverseOpt.get();

			PartialReversalRequest request = aciRequestFactory.createPartialReversal(gpasRequest, merchantMaster, authLog, authLogToReverse);

			val startTime = Instant.now();
			val childGuid = gpasRequest.getTransactionHeader().getChildGUID();
			log.info("Pratial Reversal request to ACI bank service {}, for child guid {}", request, childGuid);
			audit.info("Started calling ACI bank service for partialReversal at {}, child guid {}", startTime, childGuid);

			val response = aciGatewayClient.partialReversal(request);

			Instant endTime = Instant.now();
			final long duration = ChronoUnit.MILLIS.between(startTime, endTime);
			log.info("Partial Reversal response from ACI bank service {}, child guid {}", response, childGuid);
			audit.info("Finished calling ACI bank service for partialReversal at {}, duration {} ms, child guid {}", endTime, duration, childGuid);

			authLog = handlePaymentResponse(authLog, response, null);

			return authLog;
		}
		else
		{
			throw new InvalidInputException("ReversalGUID did not match any row in the database.");
		}
	}

	private AuthLog handleBalanceResponse(AuthLog authLog, @Nullable ResponseWrapper<AciBalanceResponse> balanceResponse)
	{
		if(balanceResponse == null || balanceResponse.getResponseBody() == null)
		{
			throw new RuntimeException("Balance Response from bank was null");
		}

		AciAuthResponse responseEntity = saveBalanceResponse(balanceResponse, authLog.getGpasKey());
		authLog = updateAuthLogWithBalanceResponse(balanceResponse, authLog, responseEntity);

		return authLog;
	}

	private AuthLog handlePaymentResponse(AuthLog authLog, @Nullable ResponseWrapper<AciPaymentResponse> paymentResponse, EntryMode entryMode)
	{
		if(paymentResponse == null || paymentResponse.getResponseBody() == null)
		{
			throw new RuntimeException("Payment Response from bank was null");
		}

		AciAuthResponse responseEntity = savePaymentResponse(paymentResponse, authLog.getGpasKey(), entryMode);
		authLog = updateAuthLogWith(paymentResponse, authLog, responseEntity);
		return authLog;
	}

	private AciAuthResponse savePaymentResponse(ResponseWrapper<AciPaymentResponse> responseWrapper, String gpasKey, EntryMode entryMode)
	{
		AciAuthResponse responseEntity = createPaymentResponseEntity(responseWrapper, gpasKey, entryMode);

		aciResponseRepo.insert(responseEntity);

		return responseEntity;
	}

	private AciAuthResponse createPaymentResponseEntity(ResponseWrapper<AciPaymentResponse> responseWrapper, String gpasKey, EntryMode entryMode)
	{
		AciPaymentResponse response = responseWrapper.getResponseBody();

		AciAuthResponseBuilder responseBuilder = AciAuthResponse.builder()
				.gpasKey(gpasKey)
				.targetUri(responseWrapper.getTargetUri())
				.httpStatus(responseWrapper.getStatusCode())
				.timeSent(responseWrapper.getTimeSent())
				.timeReceived(responseWrapper.getTimeReceived())
				.entryMode(entryMode)
				.aciTransactionId(response.getId())
				.responseTimestamp(response.getTimestamp())
				.amount(response.getAmount())
				.currency(response.getCurrency())
				.tokenId(response.getTokenId());

		PaymentResult result = response.getResult();
		if(result != null)
		{
			responseBuilder
					.resultCode(result.getCode())
					.resultDescription(result.getDescription())
					.avsResponse(result.getAvsResponse())
					.cvvResponse(result.getCvvResponse())
					.authorizationId(result.getAuthorizationId());
		}

		PaymentResultDetails resultDetails = response.getResultDetails();
		if(resultDetails != null)
		{
			final String customDetails = flattenDetails(resultDetails.getCustomDetails());

			responseBuilder
					.retrievalReferenceNumber(resultDetails.getRetrievalReferenceNumber())
					.acquirerResponse(resultDetails.getAcquirerResponse())
					.acquirerAvsResponse(resultDetails.getAcquirerAvsResponse())
					.commercialCardIndicator(resultDetails.getCommercialCardIndicator())
					.level3InterchangeEligible(resultDetails.getLevel3InterchangeEligible())
					.retailReturnHash(resultDetails.getRetailReturnHash())
					.panKHash(resultDetails.getPanKHash())
					.bankTransactionId(resultDetails.getBankTransactionId())
					.banknetDate(resultDetails.getBanknetDate())
					.banknetRefNr(resultDetails.getBanknetRefNr())
					.resultCustomDetails(customDetails);
		}

		Balances balances = response.getBalances();
		if(balances != null)
		{
			responseBuilder.availableBalance(balances.getAvailableBalance());
		}

		ThreeDSecureResult threeDSecure = response.getThreeDSecure();
		if(threeDSecure != null && threeDSecure.getResult() != null)
		{
			responseBuilder
					.threeDSecureResult(threeDSecure.getResult())
					.cavvResultCode(threeDSecure.getResult().getCavvResultCode());
		}

		CustomParameters customParamWrapper = response.getCustomParameters();
		if(customParamWrapper != null)
		{
			String customParams = flattenDetails(customParamWrapper.getCustomParameters());

			responseBuilder.customParameters(customParams);
		}

		AciAuthResponse responseEntity = responseBuilder.build();
		return responseEntity;
	}

	private AciAuthResponse saveBalanceResponse(ResponseWrapper<AciBalanceResponse> responseWrapper, String gpasKey)
	{
		AciAuthResponse responseEntity = createBalanceResponseEntity(responseWrapper, gpasKey);

		aciResponseRepo.insert(responseEntity);

		return responseEntity;
	}

	private AciAuthResponse createBalanceResponseEntity(ResponseWrapper<AciBalanceResponse> responseWrapper, String gpasKey)
	{
		AciBalanceResponse response = responseWrapper.getResponseBody();

		AciAuthResponseBuilder responseBuilder = AciAuthResponse.builder()
				.gpasKey(gpasKey)
				.targetUri(responseWrapper.getTargetUri())
				.httpStatus(responseWrapper.getStatusCode())
				.timeSent(responseWrapper.getTimeSent())
				.timeReceived(responseWrapper.getTimeReceived())
				.aciTransactionId(response.getId())
				.responseTimestamp(response.getTimestamp())
				.amount(response.getAmount())
				.currency(response.getCurrency())
				.tokenId(response.getTokenId());

		BalanceResult result = response.getResult();
		if(result != null)
		{
			responseBuilder
					.resultCode(result.getCode())
					.resultDescription(result.getDescription())
					.cvvResponse(result.getCvvResponse())
					.authorizationId(result.getAuthorizationId());
		}

		BalanceResultDetails resultDetails = response.getResultDetails();
		if(resultDetails != null)
		{
			final String customDetails = flattenDetails(resultDetails.getCustomDetails());

			responseBuilder
					.retrievalReferenceNumber(resultDetails.getRetrievalReferenceNumber())
					.acquirerResponse(resultDetails.getAcquirerResponse())
					.acquirerAvsResponse(resultDetails.getAcquirerAvsResponse())
					.commercialCardIndicator(resultDetails.getCommercialCardIndicator())
					.level3InterchangeEligible(resultDetails.getLevel3InterchangeEligible())
					.resultCustomDetails(customDetails);
		}

		Balances balances = response.getBalances();
		if(balances != null)
		{
			responseBuilder.availableBalance(balances.getAvailableBalance());
		}

		CustomParameters customParamWrapper = response.getCustomParameters();
		if(customParamWrapper != null)
		{
			String customParams = flattenDetails(customParamWrapper.getCustomParameters());

			responseBuilder.customParameters(customParams);
		}

		AciAuthResponse responseEntity = responseBuilder.build();
		return responseEntity;
	}

	private @Nullable String flattenDetails(@Nullable Map<String, String> detailsMap)
	{
		if(detailsMap != null)
		{
			// We decided it was fine to use toString as we don't need to convert back to object/map, it is human readable, and it doesn't take as much space as JSON
			return detailsMap.toString();
		}
		else
		{
			return null;
		}
	}

	private AuthLog updateAuthLogWith(ResponseWrapper<AciPaymentResponse> responseWrapper, AuthLog authLog, AciAuthResponse responseEntity) // TODO: Pratima wants us to save the card type ACI returns to us and send it back to the customer
	{
		String childGuid = authLog.getChildKey();

		AciPaymentResponse response = responseWrapper.getResponseBody();
		PaymentResult result = response.getResult(); // TODO: need to determine how to best handle if result is null
		Balances balances = response.getBalances();

		log.info("{}", response);

		authLog.setRespRcvdFromBankDatetime(responseWrapper.getTimeReceived());
		authLog.setRequestSentToBankDatetime(responseWrapper.getTimeSent());

		RespResponseInfo responseInfoResp = responseInfoCache.getRespResponseInfoBy(bankId, result.getCode(), null, result.getDescription());
		ReasonResponseInfo responseInfoReas = responseInfoCache.getReasonResponseInfoBy(bankId, result.getCode(), null, result.getDescription());

		CcinResponseInfo responseInfoCCIN;
		try
		{
			responseInfoCCIN = (CcinResponseInfo) responseInfoCache.getCcinResponseInfoBy(bankId, result.getCvvResponse(), null, null);
		}
		catch(MissingRespInfoException e)
		{
			log.warn("childGuid = {}, CCIN response code {} not found in database, switch to CCIN response code of null", childGuid, result.getCvvResponse());
			responseInfoCCIN = (CcinResponseInfo) responseInfoCache.getCcinResponseInfoBy(bankId, null, null, null);
		}

		AvsResponseInfo responseInfoAvs;
		try
		{
			responseInfoAvs = responseInfoCache.getAvsResponseInfoBy(bankId, result.getAvsResponse(), null, null);
		}
		catch(MissingRespInfoException e)
		{
			log.error("childGuid = {}, AVS response code {} not found in database, switch to AVS response code of null", childGuid, result.getAvsResponse());
			responseInfoAvs = responseInfoCache.getAvsResponseInfoBy(bankId, null, null, null);
		}

		log.info("{}", responseInfoResp);
		log.info("{}", responseInfoReas);
		log.info("{}", responseInfoCCIN);
		log.info("{}", responseInfoAvs);

		// Temporary fix. 85 is a Fiserv response code that means no reason to reject the card based off of the AVS response.
		// As such we need to change the normalization for the AVS code in that scenario.
		// TODO: We need to go to ACI and ask them for a more permanent solution to this.
		if(authLog.getRequestType() == AuthRequestType.PreAuthorization && GpasRespCode.A == responseInfoResp.getGpasCode() && "00".equals(responseInfoReas.getGpasCode())
				&& "85".equals(response.getResultDetails().getAcquirerResponse()) && responseInfoAvs.getGpasCode() != AvsResponseCode.Y
				&& List.of(PaymentMethod.VI, PaymentMethod.MC, PaymentMethod.DI).contains(authLog.getMerchantMaster().getPaymentMethod()))
		{
			audit.info("childGuid = {}, ACI AVS response code {} normalized as Y instead of the regular {} because Fiserv acquirer repsonse code 85 was received.", childGuid,
					result.getAvsResponse(), responseInfoAvs.getGpasCode());
			authLog.setGpasAvsCode(AvsResponseCode.Y);
		}
		else
		{
			authLog.setGpasAvsCode(responseInfoAvs.getGpasCode());
		}

		authLog.setGpasCvvCode(responseInfoCCIN.getGpasCode());
		authLog.setGpasReasCode(responseInfoReas.getGpasCode());
		authLog.setGpasRespCode(responseInfoResp.getGpasCode());

		authLog.setVendorAvsCode(responseInfoAvs.getBankCode());
		authLog.setVendorCvvCode(responseInfoCCIN.getBankCode());
		authLog.setVendorReasCode(responseInfoReas.getBankCode());
		authLog.setVendorRespCode(responseInfoResp.getBankCode());

		final String gpasResponseDesc = responseInfoResp.getGpasCodeDesc() + " " + responseInfoReas.getGpasCodeDesc();
		authLog.setGpasResponseDescription(gpasResponseDesc);

		// in case of an error response which is mapped at our end

		final BigDecimal approvedAmount;

		if(GpasRespCode.A == responseInfoResp.getGpasCode())
		{
			approvedAmount = response.getAmount();

			if(authLog.getRequestType() == AuthRequestType.PreAuthorization && Strings.isBlank(result.getAuthorizationId()))
			{
				authLog.setAuthCode("GPAS01");
			}
			else
			{
				authLog.setAuthCode(result.getAuthorizationId());
			}
		}
		else
		{
			approvedAmount = new BigDecimal("0.00");
		}

		authLog.setApprovedAmount(approvedAmount);

		final BigDecimal remainingBalance;

		if(authLog.getPaymentType() != PaymentType.Credit && List.of(AuthRequestType.Authorization, AuthRequestType.ReAuthorization,
				AuthRequestType.AuthorizationComplete, AuthRequestType.Reversal).contains(authLog.getRequestType())
				&& balances != null && balances.getAvailableBalance() != null)
		{
			remainingBalance = balances.getAvailableBalance();
		}
		else
		{
			remainingBalance = new BigDecimal("0.00");
		}
		authLog.setRemainingBalanceAmount(remainingBalance);

		authLog.setMessageStatus(MessageStatus.Successful);
		authLog.setAciResponse(responseEntity);// Setting here doesn't effect db, rather it ensures that the object in memory matches the db

		val threeDSResponse = authLog.getThreeDSResponse();
		if(response.getResultDetails() != null && threeDSResponse != null)
		{
			threeDSResponse.setAuthCharInd(response.getResultDetails().getAuthCharIndicator());
			if(response.getThreeDSecure() != null)
			{
				threeDSResponse.setThreedsAuthResponse(response.getThreeDSecure().getResult());
			}
		}

		try
		{
			authLogRepo.update(authLog);
		}
		catch(Exception e)
		{
			log.error("Saving authLog failed. {}", authLog);
			throw e;
		}

		return authLog;
	}

	private AuthLog updateAuthLogWithBalanceResponse(ResponseWrapper<AciBalanceResponse> responseWrapper, AuthLog authLog, AciAuthResponse responseEntity)
	{
		AciBalanceResponse response = responseWrapper.getResponseBody();

		BalanceResult result = response.getResult();// TODO: need to determine how to best handle if result is null
		Balances balances = response.getBalances();

		authLog.setRespRcvdFromBankDatetime(responseWrapper.getTimeReceived());
		authLog.setRequestSentToBankDatetime(responseWrapper.getTimeSent());

		RespResponseInfo responseInfoResp = responseInfoCache.getRespResponseInfoBy(bankId, result.getCode(), null, result.getDescription());
		ReasonResponseInfo responseInfoReas = responseInfoCache.getReasonResponseInfoBy(bankId, result.getCode(), null, result.getDescription());

		CcinResponseInfo responseInfoCCIN;
		try
		{
			responseInfoCCIN = responseInfoCache.getCcinResponseInfoBy(bankId, result.getCvvResponse(), null, null);
		}
		catch(MissingRespInfoException e)
		{
			log.warn("childGuid = {}, CCIN response code {} not found in database, switch to CCIN response code of null", authLog.getChildKey(), result.getCvvResponse());
			responseInfoCCIN = responseInfoCache.getCcinResponseInfoBy(bankId, null, null, null);
		}

		authLog.setGpasAvsCode(AvsResponseCode.U); // no avs response for Balance Inquiry calls, hence setting up manually to align with Legacy
		authLog.setGpasCvvCode(responseInfoCCIN.getGpasCode());
		authLog.setGpasReasCode(responseInfoReas.getGpasCode());
		authLog.setGpasRespCode(responseInfoResp.getGpasCode());

		authLog.setVendorCvvCode(responseInfoCCIN.getBankCode());
		authLog.setVendorReasCode(responseInfoReas.getBankCode());
		authLog.setVendorRespCode(responseInfoResp.getBankCode());

		final String gpasResponseDesc = responseInfoResp.getGpasCodeDesc() + " " + responseInfoReas.getGpasCodeDesc();
		authLog.setGpasResponseDescription(gpasResponseDesc);

		// in case of an error response which is mapped at our end

		final BigDecimal approvedAmount;
		final BigDecimal remainingBalance;
		if(GpasRespCode.A == responseInfoResp.getGpasCode())
		{
			approvedAmount = response.getAmount();

			if(authLog.getPaymentType() == PaymentType.Prepaid && Strings.isBlank(result.getAuthorizationId()))
			{
				authLog.setAuthCode("GPAS01");
			}
			else
			{
				authLog.setAuthCode(result.getAuthorizationId());
			}

			if(balances != null && balances.getAvailableBalance() != null)
			{
				remainingBalance = balances.getAvailableBalance();
			}
			else
			{
				remainingBalance = new BigDecimal("0.00");
			}
		}
		else
		{
			approvedAmount = new BigDecimal("0.00");
			remainingBalance = new BigDecimal("0.00");
		}

		authLog.setApprovedAmount(approvedAmount);
		authLog.setRemainingBalanceAmount(remainingBalance);

		authLog.setMessageStatus(MessageStatus.Successful);
		authLog.setAciResponse(responseEntity);// Setting here doesn't effect db, rather it ensures that the object in memory matches the db

		try
		{
			authLogRepo.update(authLog);
		}
		catch(Exception e)
		{
			log.error("Saving authLog failed. {}", authLog);
			throw e;
		}

		return authLog;
	}
}