package com.staples.payment.authorization.service.bank.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.staples.payment.authorization.clients.CybersourceAuthClient;
import com.staples.payment.authorization.dto.cybersource.ReasonDetails;
import com.staples.payment.authorization.exception.BankCommTimeoutException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.service.bank.CybersourcePaymentService;
import com.staples.payment.authorization.service.factory.CybersourceRequestFactory;
import com.staples.payment.shared.cache.ResponseInfoCache;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.Bank;
import com.staples.payment.shared.constant.CybSuccessResponse;
import com.staples.payment.shared.constant.GpasRespCode;
import com.staples.payment.shared.constant.MessageStatus;
import com.staples.payment.shared.constant.PaymentType;
import com.staples.payment.shared.cybersource.dto.request.CSPaymentsRequest;
import com.staples.payment.shared.cybersource.dto.request.CSReversalRequest;
import com.staples.payment.shared.cybersource.dto.response.CSPaymentsResponse;
import com.staples.payment.shared.cybersource.dto.response.CSReversalsResponse;
import com.staples.payment.shared.cybersource.dto.response.ResponseWrapper;
import com.staples.payment.shared.cybersource.dto.response.common.AmountDetails;
import com.staples.payment.shared.cybersource.dto.response.common.Avs;
import com.staples.payment.shared.cybersource.dto.response.common.ErrorInformation;
import com.staples.payment.shared.cybersource.dto.response.common.ProcessorInformation;
import com.staples.payment.shared.cybersource.dto.response.reversal.ReversalAmountDetails;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.MerchantMaster;
import com.staples.payment.shared.entity.cybersource.CybersourceAuthResponse;
import com.staples.payment.shared.entity.cybersource.CybersourceAuthResponse.CybersourceAuthResponseBuilder;
import com.staples.payment.shared.entity.respInfo.AvsResponseInfo;
import com.staples.payment.shared.entity.respInfo.ReasonResponseInfo;
import com.staples.payment.shared.entity.respInfo.RespResponseInfo;
import com.staples.payment.shared.exceptions.MissingRespInfoException;
import com.staples.payment.shared.repo.AuthLogRepo;
import com.staples.payment.shared.repo.bank.CybersourceAuthResponseRepo;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CybersourcePaymentServiceImpl implements CybersourcePaymentService
{

	private static final Logger audit = LoggerFactory.getLogger("audit-log");

	private final CybersourceAuthClient cybersourceAuthClient;
	private final ResponseInfoCache responseInfoCache;
	private final AuthLogRepo authLogRepo;
	private final CybersourceAuthResponseRepo cybAuthResponseRepo;
	private final CybersourceRequestFactory cybersourceRequestFactory;

	private final Bank bankId = Bank.CYB;

	public CybersourcePaymentServiceImpl(CybersourceAuthClient cybersourceAuthClient, ResponseInfoCache responseInfoCache, AuthLogRepo authLogRepo, CybersourceAuthResponseRepo cybAuthResponseRepo,
			CybersourceRequestFactory cybersourceRequestFactory)
	{
		super();
		this.cybersourceAuthClient = cybersourceAuthClient;
		this.responseInfoCache = responseInfoCache;
		this.authLogRepo = authLogRepo;
		this.cybAuthResponseRepo = cybAuthResponseRepo;
		this.cybersourceRequestFactory = cybersourceRequestFactory;
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
				throw new RuntimeException("invalid payment type for Cybersource");
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

		if(List.of(AuthRequestType.PreAuthorization, AuthRequestType.Authorization, AuthRequestType.ReAuthorization).contains(requestType))
		{
			ResponseWrapper<CSPaymentsResponse> paymentsResponse = payments(gpasRequest, merchantMaster, authLog.getGpasKey());

			authLog = handleCybersourceResponse(authLog, paymentsResponse, gpasRequest);
		}
		else if(requestType == AuthRequestType.Reversal)
		{
			authLog = processReversalRequest(authLog, merchantMaster, gpasRequest);
		}
		else
		{
			throw new RuntimeException("invalid request type for Cybersource");
		}

		return authLog;
	}

	private AuthLog processReversalRequest(AuthLog authLog, MerchantMaster merchantMaster, AuthRequest gpasRequest)
	{
		val transactionHeader = gpasRequest.getTransactionHeader();
		val reversalGuid = transactionHeader.getReversalGUID();
		val childGuid = transactionHeader.getChildGUID();

		Optional<AuthLog> originalAuthLogOptional = authLogRepo.findById(reversalGuid);

		if(originalAuthLogOptional.isPresent())
		{
			AuthLog originalAuthLog = originalAuthLogOptional.get();

			CybersourceAuthResponse cybAuthRespone = originalAuthLog.getCybersourceResponse();

			if(cybAuthRespone != null)
			{
				ResponseWrapper<CSReversalsResponse> reversalsResponseWrapper = reversals(gpasRequest, cybAuthRespone, merchantMaster, authLog.getGpasKey());
				authLog = handleReversalsResponse(authLog, cybAuthRespone, reversalsResponseWrapper, gpasRequest);
			}
			else
			{
				throw new RuntimeException(
						"no cybersource response for the original transaction for which the Reversal is tried, with gpasKey : " + originalAuthLog.getGpasKey() + " and childGuid : " + childGuid);
			}
		}
		else
		{
			throw new RuntimeException("original transaction is not present in database for the Reversal, where reversalGuid : " + reversalGuid + " and childGuid : " + childGuid);
		}

		return authLog;
	}

	private ResponseWrapper<CSPaymentsResponse> payments(AuthRequest gpasRequest, MerchantMaster merchantMaster, String gpasKey)
	{
		Map<String, String> headers = cybersourceRequestFactory.createHeaders(gpasKey, merchantMaster);
		CSPaymentsRequest request = cybersourceRequestFactory.createPaymentRequest(gpasRequest);

		val startTime = Instant.now();
		val childGuid = gpasRequest.getTransactionHeader().getChildGUID();
		val requestType = gpasRequest.getTransactionHeader().getRequestType();
		audit.info("Child GUID = {} with RequestType = {}, started calling Cybersource bank service at {} ", childGuid, requestType, startTime);

		val paymentResponse = cybersourceAuthClient.payments(headers, request);

		Instant endTime = Instant.now();
		audit.info("Child GUID = {} with Response Satus Code = {}, finished calling Cybersource bank service at {} ", childGuid,
				paymentResponse.getStatusCode(), endTime);
		audit.info("Childguid: " + childGuid + ", Time taken for Cybersource bank call: " + ChronoUnit.MILLIS.between(startTime, endTime) + " ms");

		return paymentResponse;
	}

	private ResponseWrapper<CSReversalsResponse> reversals(AuthRequest gpasRequest, CybersourceAuthResponse cybAuthRespone, MerchantMaster merchantMaster, String gpasKey)
	{
		Map<String, String> headers = cybersourceRequestFactory.createHeaders(gpasKey, merchantMaster);
		CSReversalRequest request = cybersourceRequestFactory.createReversalRequest(gpasRequest, cybAuthRespone);

		val startTime = Instant.now();
		val childGuid = gpasRequest.getTransactionHeader().getChildGUID();
		val requestType = gpasRequest.getTransactionHeader().getRequestType();
		audit.info("Child GUID = {} with RequestType = {}, started calling Cybersource bank service at {} ", childGuid, requestType, startTime);

		val reversalResponse = cybersourceAuthClient.reversals(headers, request, cybAuthRespone.getAuthReferenceId());

		Instant endTime = Instant.now();
		audit.info("Child GUID = {} with Response Satus Code = {}, finished calling Cybersource bank service at {} ", childGuid,
				reversalResponse.getStatusCode(), endTime);
		audit.info("Childguid: " + childGuid + ", Time taken for Cybersource bank call: " + ChronoUnit.MILLIS.between(startTime, endTime) + " ms");

		return reversalResponse;
	}

	private AuthLog handleCybersourceResponse(AuthLog authLog, ResponseWrapper<CSPaymentsResponse> response, AuthRequest gpasRequest)
	{
		if(response == null || response.getResponseBody() == null)
		{
			throw new RuntimeException("Payment Response from Cybersource is null");
		}

		CybersourceAuthResponse paymentsResponseEntity = savePaymentsResponse(response, authLog.getGpasKey(), gpasRequest);
		authLog = updateAuthLogWithPaymentsResponse(response, authLog, paymentsResponseEntity);

		return authLog;
	}

	private CybersourceAuthResponse savePaymentsResponse(ResponseWrapper<CSPaymentsResponse> response, String gpasKey, AuthRequest gpasRequest)
	{
		try
		{
			CSPaymentsResponse paymentResponse = response.getResponseBody();

			val clientReferenceInformation = paymentResponse.getClientReferenceInformation();
			val orderInformation = paymentResponse.getOrderInformation();
			val processorInformation = paymentResponse.getProcessorInformation();
			val errorInformation = paymentResponse.getErrorInformation();
			val errorDetails = paymentResponse.getDetails();

			CybersourceAuthResponseBuilder authResponseBuilder = CybersourceAuthResponse.builder()
					.gpasKey(gpasKey)
					.transactionType(gpasRequest.getTransactionHeader().getRequestType())
					.responseStatus(response.getStatusCode())
					.clientReferenceCode(null != clientReferenceInformation ? clientReferenceInformation.getCode() : null)
					.authReferenceId(paymentResponse.getId())
					.responseCode(String.valueOf(response.getStatusCode().value()));
			if(orderInformation != null)
			{
				val amountDetails = paymentResponse.getOrderInformation().getAmountDetails();
				if(amountDetails != null)
				{
					authResponseBuilder
							.authorizedAmount(amountDetails.getAuthorizedAmount())
							.currency(amountDetails.getCurrency());
				}
			}
			if(processorInformation != null)
			{
				authResponseBuilder
						.approvalCode(processorInformation.getApprovalCode())
						.networkTransactionId(processorInformation.getNetworkTransactionId())
						.transactionId(processorInformation.getTransactionId())
						.avsCode(processorInformation.getAvs().getCode())
						.avsCodeRaw(processorInformation.getAvs().getCodeRaw());
			}

			if(errorInformation != null)
			{
				authResponseBuilder
						.reasonDescription(errorInformation.getReason())
						.messageDetails(errorInformation.getMessage());
			}
			else
			{
				authResponseBuilder
						.reasonDescription(paymentResponse.getReason())
						.messageDetails(paymentResponse.getMessage());
			}

			if(errorDetails != null)
			{
				authResponseBuilder.errorDetails(errorDetails.toString());
			}

			authResponseBuilder
					.status(paymentResponse.getStatus())
					.requestSentTime(response.getTimeSent())
					.responseReceivedTime(response.getTimeReceived());

			CybersourceAuthResponse paymentResponseEntity = authResponseBuilder.build();

			cybAuthResponseRepo.insert(paymentResponseEntity);

			return paymentResponseEntity;
		}
		catch(Exception ex)
		{
			// TODO: need to remove this try-catch along with error logger, later release
			log.error("Saving response details failed. {}", ex);
			throw ex;
		}
	}

	private AuthLog updateAuthLogWithPaymentsResponse(ResponseWrapper<CSPaymentsResponse> response, AuthLog authLog, CybersourceAuthResponse paymentsResponseEntity)
	{
		final String childGuid = authLog.getChildKey();

		CSPaymentsResponse authResponse = response.getResponseBody();

		log.info("{}", authResponse);

		val processorInformation = authResponse.getProcessorInformation();
		val orderInformation = authResponse.getOrderInformation();

		final String approvalCode = null != processorInformation ? processorInformation.getApprovalCode() : null;

		final String avsCode = getAvsCode(processorInformation);

		val errorInformation = authResponse.getErrorInformation();

		// TODO : think about converting responseStatus as enum then Success Response enum can be a subset of that, later release
		// It will be hard to determine but the json page as shared by Randy can be considered here, will think about this before CTG release
		final String responseStatus = authResponse.getStatus();
		ReasonDetails reasonDetails = getReasonDetails(authResponse, errorInformation, responseStatus);

		RespResponseInfo responseInfoResp = responseInfoCache.getRespResponseInfoBy(bankId, responseStatus, null, responseStatus);
		ReasonResponseInfo responseInfoReas = responseInfoCache.getReasonResponseInfoBy(bankId, reasonDetails.getCode(), null, reasonDetails.getDescription());

		authLog.setRespRcvdFromBankDatetime(response.getTimeReceived());
		authLog.setRequestSentToBankDatetime(response.getTimeSent());

		AvsResponseInfo responseInfoAvs;
		try
		{
			responseInfoAvs = responseInfoCache.getAvsResponseInfoBy(bankId, avsCode, null, null);
		}
		catch(MissingRespInfoException e)
		{
			log.error("childGuid = {}, AVS response code {} not found in database, switch to AVS response code of null", childGuid, avsCode);
			responseInfoAvs = responseInfoCache.getAvsResponseInfoBy(bankId, null, null, null);
		}

		log.info("{}", responseInfoResp);
		log.info("{}", responseInfoReas);
		log.info("{}", responseInfoAvs);

		authLog.setGpasAvsCode(responseInfoAvs.getGpasCode());
		authLog.setGpasCvvCode(null);
		authLog.setGpasReasCode(responseInfoReas.getGpasCode());
		authLog.setGpasRespCode(responseInfoResp.getGpasCode());

		authLog.setVendorAvsCode(responseInfoAvs.getBankCode());
		authLog.setVendorCvvCode(null);
		authLog.setVendorReasCode(responseInfoReas.getBankCode());
		authLog.setVendorRespCode(responseInfoResp.getBankCode());

		final String gpasResponseDesc = responseInfoResp.getGpasCodeDesc() + " " + responseInfoReas.getGpasCodeDesc();
		authLog.setGpasResponseDescription(gpasResponseDesc);

		AmountDetails amountDetails = null != orderInformation ? orderInformation.getAmountDetails() : null;
		final BigDecimal approvedAmount;
		if(GpasRespCode.A == responseInfoResp.getGpasCode())
		{
			if(amountDetails != null)
			{
				approvedAmount = amountDetails.getAuthorizedAmount();
			}
			else
			{
				approvedAmount = authLog.getTransactionAmount();
			}
			authLog.setAuthCode(approvalCode);
		}
		else
		{
			approvedAmount = new BigDecimal("0.00");
		}

		authLog.setApprovedAmount(approvedAmount);

		authLog.setMessageStatus(MessageStatus.Successful);
		authLog.setCybersourceResponse(paymentsResponseEntity); // Setting here doesn't effect db, rather it ensures that the object in memory matches the db

		try
		{
			authLogRepo.update(authLog);
		}
		catch(Exception e)
		{
			log.error("Updating authLog with Cybersource payments response got failed. {}", authLog);
			throw e;
		}

		return authLog;
	}

	private String getAvsCode(ProcessorInformation processorInformation)
	{
		Avs avs = null;
		if(processorInformation != null)
		{
			avs = processorInformation.getAvs();
		}

		return null != avs ? avs.getCode() : null;
	}

	private ReasonDetails getReasonDetails(Object response, ErrorInformation errorInformation, String responseStatus)
	{
		final String reasonCode;
		String reasonDesc = null;

		if(errorInformation != null)
		{
			reasonCode = null != errorInformation.getReason() ? errorInformation.getReason() : responseStatus;
			reasonDesc = errorInformation.getMessage();
		}
		else if(response instanceof CSPaymentsResponse && !List.of(CybSuccessResponse.approved, CybSuccessResponse.partailApproved).contains(responseStatus))
		{
			reasonCode = ((CSPaymentsResponse) response).getReason();
			reasonDesc = ((CSPaymentsResponse) response).getMessage();
		}

		else if(response instanceof CSReversalsResponse && !CybSuccessResponse.reversed.equals(responseStatus))
		{
			reasonCode = ((CSReversalsResponse) response).getReason();
			reasonDesc = ((CSReversalsResponse) response).getMessage();
		}

		else
		{
			reasonCode = responseStatus;
		}

		return ReasonDetails.builder()
				.code(reasonCode)
				.description(reasonDesc)
				.build();
	}

	private AuthLog handleReversalsResponse(AuthLog authLog, CybersourceAuthResponse cybAuthRespone, ResponseWrapper<CSReversalsResponse> reversalsResponseWrapper, AuthRequest gpasRequest)
	{
		if(reversalsResponseWrapper == null || reversalsResponseWrapper.getResponseBody() == null)
		{
			throw new RuntimeException("Reversal Response from Cybersource is null");
		}

		CybersourceAuthResponse reversalResponseEntity = saveReversalsResponse(reversalsResponseWrapper, cybAuthRespone, authLog.getGpasKey(), gpasRequest);
		authLog = updateAuthLogWithReversalsResponse(reversalsResponseWrapper, authLog, reversalResponseEntity);

		return authLog;
	}

	private CybersourceAuthResponse saveReversalsResponse(ResponseWrapper<CSReversalsResponse> response, CybersourceAuthResponse cybAuthRespone, String gpasKey,
			AuthRequest gpasRequest)
	{
		try
		{
			CSReversalsResponse reversalsResponse = response.getResponseBody();

			val clientReferenceInformation = reversalsResponse.getClientReferenceInformation();
			val reversalAmountDetails = reversalsResponse.getReversalAmountDetails();
			val processorInformation = reversalsResponse.getProcessorInformation();
			val errorInformation = reversalsResponse.getErrorInformation();
			val errorDetails = reversalsResponse.getDetails();

			CybersourceAuthResponseBuilder reversalsResponseBuilder = CybersourceAuthResponse.builder()
					.gpasKey(gpasKey)
					.transactionType(gpasRequest.getTransactionHeader().getRequestType())
					.responseStatus(response.getStatusCode())
					.clientReferenceCode(null != clientReferenceInformation ? clientReferenceInformation.getCode() : null)
					.authReferenceId(reversalsResponse.getId())
					.responseCode(String.valueOf(response.getStatusCode().value()));
			if(reversalAmountDetails != null)
			{
				reversalsResponseBuilder
						.authorizedAmount(reversalAmountDetails.getReversedAmount())
						.currency(reversalAmountDetails.getCurrency());
			}
			if(processorInformation != null)
			{
				reversalsResponseBuilder
						.approvalCode(cybAuthRespone.getApprovalCode());
			}
			if(errorInformation != null)
			{
				reversalsResponseBuilder
						.reasonDescription(errorInformation.getReason())
						.messageDetails(errorInformation.getMessage());
			}
			else
			{
				reversalsResponseBuilder
						.reasonDescription(reversalsResponse.getReason())
						.messageDetails(reversalsResponse.getMessage());
			}

			if(errorDetails != null)
			{
				reversalsResponseBuilder.errorDetails(errorDetails.toString());
			}

			reversalsResponseBuilder
					.status(reversalsResponse.getStatus())
					.requestSentTime(response.getTimeSent())
					.responseReceivedTime(response.getTimeReceived());

			CybersourceAuthResponse reversalsResponseEntity = reversalsResponseBuilder.build();

			cybAuthResponseRepo.insert(reversalsResponseEntity);

			return reversalsResponseEntity;
		}
		catch(Exception ex)
		{
			log.error("Saving response details failed. {}", ex);
			throw ex;
		}
	}

	private AuthLog updateAuthLogWithReversalsResponse(ResponseWrapper<CSReversalsResponse> reversalsResponseWrapper, AuthLog authLog, CybersourceAuthResponse responseEntity)
	{
		CSReversalsResponse reversalsResponse = reversalsResponseWrapper.getResponseBody();

		log.info("{}", reversalsResponse);

		val processorInformation = reversalsResponse.getProcessorInformation();

		final String approvalCode = null != processorInformation ? processorInformation.getApprovalCode() : null;

		val errorInformation = reversalsResponse.getErrorInformation();

		final String responseStatus = reversalsResponse.getStatus();
		ReasonDetails reasonDetails = getReasonDetails(reversalsResponse, errorInformation, responseStatus);

		RespResponseInfo responseInfoResp = responseInfoCache.getRespResponseInfoBy(bankId, responseStatus, null, responseStatus);
		ReasonResponseInfo responseInfoReas = responseInfoCache.getReasonResponseInfoBy(bankId, reasonDetails.getCode(), null, reasonDetails.getDescription());

		authLog.setRespRcvdFromBankDatetime(reversalsResponseWrapper.getTimeReceived());
		authLog.setRequestSentToBankDatetime(reversalsResponseWrapper.getTimeSent());

		log.info("{}", responseInfoResp);
		log.info("{}", responseInfoReas);

		authLog.setGpasAvsCode(null);
		authLog.setGpasCvvCode(null);
		authLog.setGpasReasCode(responseInfoReas.getGpasCode());
		authLog.setGpasRespCode(responseInfoResp.getGpasCode());

		authLog.setVendorAvsCode(null);
		authLog.setVendorCvvCode(null);
		authLog.setVendorReasCode(responseInfoReas.getBankCode());
		authLog.setVendorRespCode(responseInfoResp.getBankCode());

		final String gpasResponseDesc = responseInfoResp.getGpasCodeDesc() + " " + responseInfoReas.getGpasCodeDesc();
		authLog.setGpasResponseDescription(gpasResponseDesc);

		ReversalAmountDetails reversalAmountDetails = reversalsResponse.getReversalAmountDetails();
		final BigDecimal approvedAmount;
		if(GpasRespCode.A == responseInfoResp.getGpasCode())
		{
			if(reversalAmountDetails != null)
			{
				approvedAmount = reversalAmountDetails.getReversedAmount();
			}
			else
			{
				approvedAmount = authLog.getTransactionAmount();
			}
			authLog.setAuthCode(approvalCode);
		}
		else
		{
			approvedAmount = new BigDecimal("0.00");
		}

		authLog.setApprovedAmount(approvedAmount);

		authLog.setMessageStatus(MessageStatus.Successful);
		authLog.setCybersourceResponse(responseEntity); // Setting here doesn't effect db, rather it ensures that the object in memory matches the db

		try
		{
			authLogRepo.update(authLog);
		}
		catch(Exception e)
		{
			log.error("Updating authLog with Cybersource reversal response got failed. {}", authLog);
			throw e;
		}

		return authLog;
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

}
