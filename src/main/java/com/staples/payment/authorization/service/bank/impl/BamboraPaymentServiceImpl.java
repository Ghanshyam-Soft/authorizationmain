package com.staples.payment.authorization.service.bank.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.staples.payment.authorization.clients.BamboraAuthClient;
import com.staples.payment.authorization.exception.BankCommTimeoutException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.service.bank.BamboraPaymentService;
import com.staples.payment.authorization.service.factory.BamboraRequestFactory;
import com.staples.payment.shared.bambora.request.SaleRequest;
import com.staples.payment.shared.bambora.response.BamboraResponse;
import com.staples.payment.shared.bambora.response.BamboraResponseWrapper;
import com.staples.payment.shared.cache.ResponseInfoCache;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.Bank;
import com.staples.payment.shared.constant.GpasRespCode;
import com.staples.payment.shared.constant.MessageStatus;
import com.staples.payment.shared.constant.PaymentType;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.MerchantMaster;
import com.staples.payment.shared.entity.bambora.BamboraResp;
import com.staples.payment.shared.entity.bambora.BamboraResp.BamboraRespBuilder;
import com.staples.payment.shared.entity.respInfo.AvsResponseInfo;
import com.staples.payment.shared.entity.respInfo.CcinResponseInfo;
import com.staples.payment.shared.entity.respInfo.ReasonResponseInfo;
import com.staples.payment.shared.entity.respInfo.RespResponseInfo;
import com.staples.payment.shared.exceptions.MissingRespInfoException;
import com.staples.payment.shared.repo.AuthLogRepo;
import com.staples.payment.shared.repo.bank.BamboraResponseRepo;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BamboraPaymentServiceImpl implements BamboraPaymentService
{
	private static final Logger audit = LoggerFactory.getLogger("audit-log");

	private final BamboraAuthClient bamboraAuthClient;
	private final ResponseInfoCache responseInfoCache;
	private final AuthLogRepo authLogRepo;
	private final BamboraResponseRepo bamboraResponseRepo;
	private final BamboraRequestFactory bamboraRequestFactory;

	private final Bank bankId = Bank.BAMBORA;

	public BamboraPaymentServiceImpl(
			BamboraAuthClient bamboraAuthClient,
			ResponseInfoCache responseInfoCache,
			AuthLogRepo authLogRepo,
			BamboraResponseRepo bamboraResponseRepo,
			BamboraRequestFactory bamboraRequestFactory)
	{
		super();
		this.bamboraAuthClient = bamboraAuthClient;
		this.responseInfoCache = responseInfoCache;
		this.authLogRepo = authLogRepo;
		this.bamboraResponseRepo = bamboraResponseRepo;
		this.bamboraRequestFactory = bamboraRequestFactory;
	}

	@Override
	public AuthLog process(AuthLog authLog, AuthRequest gpasRequest, MerchantMaster merchantMaster)
	{
		try
		{
			PaymentType paymentType = gpasRequest.getTransactionHeader().getPaymentType();
			if(paymentType == PaymentType.Credit)
			{
				authLog = processCreditAuthRequest(authLog, gpasRequest, merchantMaster);
			}
			else
			{
				throw new RuntimeException("invalid payment type for BAMBORA");
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

	private AuthLog processCreditAuthRequest(AuthLog authLog, AuthRequest gpasRequest, MerchantMaster merchantMaster)
	{
		AuthRequestType requestType = gpasRequest.getTransactionHeader().getRequestType();

		if(requestType == AuthRequestType.Authorization || requestType == AuthRequestType.PreAuthorization)
		{
			BamboraResponseWrapper responseWrapper = sale(gpasRequest, merchantMaster, authLog);

			authLog = handleAuthResponse(authLog, responseWrapper);
		}
		else
		{
			throw new RuntimeException("invalid request type for BAMBORA");
		}

		return authLog;
	}

	private BamboraResponseWrapper sale(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog)
	{
		final SaleRequest request = bamboraRequestFactory.createSaleRequest(gpasRequest, merchantMaster, authLog);

		val transactionHeader = gpasRequest.getTransactionHeader();
		val childGuid = transactionHeader.getChildGUID();
		Instant startTime = Instant.now();
		audit.info("Child GUID = {} with RequestType = {}, started calling Bambora bank service at {} ", childGuid, transactionHeader.getRequestType(), startTime);

		val responseWrapper = bamboraAuthClient.sale(request); // TODO: need to send banktimeout exception here only

		Instant endTime = Instant.now();
		long timeTaken = ChronoUnit.MILLIS.between(startTime, endTime);
		audit.info("Child GUID = {} finished calling Bambora bank service at {}, Time taken for Bambora bank call: {} ms", childGuid, endTime, timeTaken);

		return responseWrapper;
	}

	private AuthLog handleAuthResponse(AuthLog authLog, BamboraResponseWrapper responseWrapper)
	{
		BamboraResp responseEntity = saveBamboraResponse(responseWrapper, authLog.getGpasKey());
		authLog = updateAuthLogWithBamboraResponse(responseWrapper, authLog, responseEntity);

		return authLog;
	}

	private BamboraResp saveBamboraResponse(BamboraResponseWrapper bamboraResponseWrapper, String gpasKey)
	{
		final BamboraResponse bamboraResponse = bamboraResponseWrapper.getResponse();
		try
		{
			final String deepValidationError = bamboraResponse.getDetails() != null ? Arrays.toString(bamboraResponse.getDetails()) : null;
			String paymentId = bamboraResponse.getId() != null ? bamboraResponse.getId() : bamboraResponse.getTransactionId() != null ? bamboraResponse.getTransactionId() : null;

			BamboraRespBuilder bamboraRespBuilder = BamboraResp.builder()
					.gpasKey(gpasKey)
					.paymentId(paymentId)
					.transactionType(bamboraResponse.getType())
					.amount(bamboraResponse.getAmount())
					.messageId(bamboraResponse.getMessageId())
					.transactionStatus(bamboraResponse.getMessage())
					.paymentMethod(bamboraResponse.getPaymentMethod())
					.riskscore(bamboraResponse.getRiskScore())
					.authCode(bamboraResponse.getAuthCode())
					.merchantId(bamboraResponse.getAuthorizingMerchantId())
					.validationError(bamboraResponse.getMessage())
					.deepValidationError(deepValidationError)
					.trxnCreatedAt(bamboraResponse.getCreated())
					.bankRespCode(bamboraResponseWrapper.getStatusCode().value());

			val card = bamboraResponse.getCard();
			if(card != null)
			{
				final String cvvResponseCode = card.getAvs() != null ? card.getAvs().getId() : null;

				bamboraRespBuilder = bamboraRespBuilder
						.addressMatch(card.getAddressMatch())
						.cvdResult(card.getCvdResult())
						.avsResult(card.getAvsResult())
						.postalResult(card.getPostalResult())
						.cvvResponseCode(cvvResponseCode);
			}

			final BamboraResp responseEntity = bamboraRespBuilder.build();
			bamboraResponseRepo.insert(responseEntity);

			return responseEntity;
		}
		catch(Exception ex)
		{
			throw new RuntimeException("Saving bambora reponse details failed. {}", ex);
		}
	}

	private AuthLog updateAuthLogWithBamboraResponse(BamboraResponseWrapper responseWrapper, AuthLog authLog, BamboraResp responseEntity)
	{
		val bamboraResponse = responseWrapper.getResponse();

		log.info("{}", bamboraResponse);

		authLog.setRespRcvdFromBankDatetime(responseWrapper.getTimeReceived());
		authLog.setRequestSentToBankDatetime(responseWrapper.getTimeSent());

		String messageId = bamboraResponse.getMessageId() != null ? bamboraResponse.getMessageId() : bamboraResponse.getCode().toString();
		RespResponseInfo responseInfoResp = responseInfoCache.getRespResponseInfoBy(bankId, messageId, null, null);
		ReasonResponseInfo responseInfoReas = responseInfoCache.getReasonResponseInfoBy(bankId, messageId, null, null);

		CcinResponseInfo responseInfoCCIN;
		AvsResponseInfo responseInfoAvs;

		String cvdResponse = bamboraResponse.getCard() != null ? bamboraResponse.getCard().getCvdResult() : null;
		String avsResponse = bamboraResponse.getCard() != null && bamboraResponse.getCard().getAvs() != null ? bamboraResponse.getCard().getAvs().getId() : null;

		try
		{
			responseInfoCCIN = responseInfoCache.getCcinResponseInfoBy(bankId, cvdResponse, null, null);
		}
		catch(MissingRespInfoException e)
		{
			log.warn("childGuid = {}, CCIN response code {} not found in database, switch to CCIN response code of null", authLog.getChildKey(), cvdResponse);
			responseInfoCCIN = responseInfoCache.getCcinResponseInfoBy(bankId, null, null, null);
		}

		try
		{
			responseInfoAvs = responseInfoCache.getAvsResponseInfoBy(bankId, avsResponse, null, null);
		}
		catch(MissingRespInfoException e)
		{
			log.error("childGuid = {}, AVS response code {} not found in database, switch to AVS response code of null", authLog.getChildKey(), avsResponse);
			responseInfoAvs = responseInfoCache.getAvsResponseInfoBy(bankId, null, null, null);
		}

		authLog.setGpasRespCode(responseInfoResp.getGpasCode());
		authLog.setGpasReasCode(responseInfoReas.getGpasCode());
		authLog.setGpasCvvCode(responseInfoCCIN.getGpasCode());
		authLog.setGpasAvsCode(responseInfoAvs.getGpasCode());

		authLog.setVendorCvvCode(cvdResponse);
		authLog.setVendorReasCode(responseInfoReas.getBankCode());
		authLog.setVendorRespCode(responseInfoResp.getBankCode());
		authLog.setVendorAvsCode(avsResponse);
		authLog.setAuthCode(bamboraResponse.getAuthCode());

		final String gpasResponseDesc = responseInfoResp.getGpasCodeDesc() + " " + responseInfoReas.getGpasCodeDesc();
		authLog.setGpasResponseDescription(gpasResponseDesc);

		final BigDecimal approvedAmount;
		if(GpasRespCode.A == responseInfoResp.getGpasCode())
		{
			approvedAmount = bamboraResponse.getAmount();
		}
		else
		{
			approvedAmount = new BigDecimal("0.00");
		}

		authLog.setApprovedAmount(approvedAmount);
		authLog.setMessageStatus(MessageStatus.Successful);
		authLog.setBamboraResponse(responseEntity);// Setting here doesn't effect db, rather it ensures that the object in memory matches the db

		try
		{
			authLogRepo.update(authLog);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Saving authLog failed.", e);
		}

		return authLog;
	}

	private void updateAuthLogForError(AuthLog authLog, MessageStatus messageStatus)
	{
		if(authLog != null)
		{
			authLog.setMessageStatus(messageStatus);
			try
			{
				authLogRepo.update(authLog);
			}
			catch(Exception e)
			{
				throw new RuntimeException("Saving updateAuthLog failed.", e);
			}
		}
	}
}