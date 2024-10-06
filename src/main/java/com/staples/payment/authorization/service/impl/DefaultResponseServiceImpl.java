package com.staples.payment.authorization.service.impl;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.staples.payment.authorization.configuration.properties.DefaultApprovalConfig;
import com.staples.payment.authorization.configuration.properties.DefaultApprovalConfig.Consumer;
import com.staples.payment.authorization.configuration.properties.FraudCheckConfig;
import com.staples.payment.authorization.configuration.properties.FraudCheckConfig.ConsumerDetail;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.details.AuthReqTransactionHeader;
import com.staples.payment.authorization.response.AuthResponse;
import com.staples.payment.authorization.service.DefaultResponseService;
import com.staples.payment.authorization.service.factory.ResponseFactory;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.GpasRespCode;
import com.staples.payment.shared.constant.MessageStatus;
import com.staples.payment.shared.constant.PaymentMethod;
import com.staples.payment.shared.constant.PaymentType;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.repo.AuthLogRepo;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DefaultResponseServiceImpl implements DefaultResponseService
{
	private static final Logger audit = LoggerFactory.getLogger("audit-log");

	private final ResponseFactory responseFactory;
	private final AuthLogRepo authLogRepo;
	private final FraudCheckConfig fraudConfig;
	private final DefaultApprovalConfig defaultApprovalConfig;

	public DefaultResponseServiceImpl(FraudCheckConfig fraudConfig, ResponseFactory responseFactory, DefaultApprovalConfig defaultApprovalConfig, AuthLogRepo authLogRepo)
	{
		this.responseFactory = responseFactory;
		this.authLogRepo = authLogRepo;
		this.fraudConfig = fraudConfig;
		this.defaultApprovalConfig = defaultApprovalConfig;
	}

	@Override
	public @Nullable AuthResponse createDefaultResponseIfNeeded(final AuthRequest request, AuthLog authLog)
	{
		val transactionHeader = request.getTransactionHeader();
		val childGuid = transactionHeader.getChildGUID();

		boolean isFraud = this.isFraudPreauth(request);
		if(isFraud)
		{
			AuthLog fraudAuthLog = updateAuthLogStatusForFraud(authLog, childGuid);
			return responseFactory.createAuthResponse(fraudAuthLog, request);
		}

		boolean shouldDefaultPreauthApproval = this.shouldDefaultPreauthApproval(request);
		if(shouldDefaultPreauthApproval)
		{
			AuthLog defaultApprovedAuthLog = updateAuthLogForDefaultApprovalPreauth(authLog, childGuid);
			return responseFactory.createAuthResponse(defaultApprovedAuthLog, request);
		}

		boolean shouldDefaultRefundApproval = this.shouldDefaultRefundApproval(request);
		if(shouldDefaultRefundApproval)
		{
			AuthLog defaultApprovedAuthLog = updateAuthLogForDefaultApprovalRefund(authLog);
			return responseFactory.createAuthResponse(defaultApprovedAuthLog, request);
		}

		return null;
	}

	private boolean shouldDefaultRefundApproval(AuthRequest request)
	{
		val transactionHeader = request.getTransactionHeader();
		val paymentType = transactionHeader.getPaymentType();
		val requestType = transactionHeader.getRequestType();
		val paymentMethod = request.getCardInfo().getPaymentType();

		if((paymentType == PaymentType.Credit || paymentType == PaymentType.Prepaid)
				&& paymentMethod == PaymentMethod.AM && requestType == AuthRequestType.Refund)
		{
			return true;
		}
		return false;
	}

	private boolean shouldDefaultPreauthApproval(final AuthRequest request)
	{
		val transactionHeader = request.getTransactionHeader();
		val paymentType = transactionHeader.getPaymentType();
		val requestType = transactionHeader.getRequestType();
		val paymentMethod = request.getCardInfo().getPaymentType();

		if(requestType == AuthRequestType.PreAuthorization && paymentType == PaymentType.Credit)
		{
			if(paymentMethod == PaymentMethod.ST || paymentMethod == PaymentMethod.SO)
			{
				for(Consumer consumer : defaultApprovalConfig.getConsumerList())
				{
					if(consumer.getBusinessUnit().equals(transactionHeader.getBusinessUnit())
							&& consumer.getDivision().equals(transactionHeader.getDivision()))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isFraudPreauth(final AuthRequest request)
	{
		val transactionHeader = request.getTransactionHeader();
		val paymentType = transactionHeader.getPaymentType();
		val requestType = transactionHeader.getRequestType();

		if(requestType == AuthRequestType.PreAuthorization && paymentType == PaymentType.Credit && fraudConfig.isEnabled())
		{
			ConsumerDetail fraudConsumer = getFraudConsumerDetails(transactionHeader);
			if(fraudConsumer != null)
			{
				boolean isFraud = isPreauthRetryLimitReached(request, fraudConsumer);
				return isFraud;
			}
		}

		return false;
	}

	private boolean isPreauthRetryLimitReached(final AuthRequest request, ConsumerDetail consumerDetail)
	{
		val transactionHeader = request.getTransactionHeader();
		val parentGuid = transactionHeader.getParentGUID();

		final Instant startTime = Instant.now();
		audit.info("beginning pre-auth retry count check for identifying fraud, start time: {}", startTime);

		int preAuthRetryCount = authLogRepo.findPreAuthRetryCount(parentGuid, request.getTransactionDetail().getPaymentToken(), consumerDetail.getDuration().toString());

		final Instant endTime = Instant.now();
		audit.info("finished pre-auth retry count check for identifying fraud, end time: {} , time taken: {} ms", endTime, Duration.between(startTime, endTime).toMillis());
		log.info("Current count in database for Parent GUID: {} is: {}", parentGuid, preAuthRetryCount);

		if(preAuthRetryCount >= consumerDetail.getRetryCount())
		{
			log.info("This Transaction seems to be fraudulent, with Parent GUID : {} and child GUID : {}", parentGuid, transactionHeader.getChildGUID());
			return true;
		}
		else
		{
			return false;
		}
	}

	private @Nullable ConsumerDetail getFraudConsumerDetails(AuthReqTransactionHeader transactionHeader)
	{
		for(ConsumerDetail consumerDetail : fraudConfig.getConsumerDetails())
		{
			if(consumerDetail.getBusinessUnit().equals(transactionHeader.getBusinessUnit())
					&& consumerDetail.getDivision().equals(transactionHeader.getDivision()))
			{
				return consumerDetail;
			}
		}
		return null;
	}

	private AuthLog updateAuthLogStatusForFraud(AuthLog authLog, String childGUID)
	{
		log.info("Updating message status to System issue in case same Parent GUID tried multiple times, hence failing Fraud Check for Child GUID {}", childGUID);

		authLog.setGpasRespCode(GpasRespCode.S);
		authLog.setGpasReasCode(fraudConfig.getGpasFraudReasonCode());
		authLog.setGpasResponseDescription(fraudConfig.getDescriptionText());
		authLog.setVendorInfoBlock(fraudConfig.getVendorInfo());
		authLog.setMessageStatus(MessageStatus.System_Issue);
		authLog.setDefaultResponse(true);

		authLogRepo.update(authLog);

		return authLog;
	}

	private AuthLog updateAuthLogForDefaultApprovalPreauth(AuthLog authLog, String childGUID)
	{
		log.info("Updating auth_log with preauth default approval for Child GUID {}", childGUID);

		authLog.setGpasRespCode(GpasRespCode.A);
		authLog.setGpasReasCode(defaultApprovalConfig.getGpasApprovalReasonCode());
		authLog.setAuthCode(defaultApprovalConfig.getAuthCode());
		authLog.setGpasResponseDescription(defaultApprovalConfig.getDescriptionText());
		authLog.setVendorInfoBlock(defaultApprovalConfig.getVendorInfo());
		authLog.setMessageStatus(MessageStatus.Successful);
		authLog.setDefaultResponse(true);

		authLogRepo.update(authLog);

		return authLog;
	}

	private AuthLog updateAuthLogForDefaultApprovalRefund(AuthLog authLog)
	{
		log.info("Updating auth_log with amex refund default approval for Child GUID {}", authLog.getChildKey());

		authLog.setApprovedAmount(authLog.getTransactionAmount());
		authLog.setGpasRespCode(GpasRespCode.A);
		authLog.setGpasReasCode(defaultApprovalConfig.getGpasApprovalReasonCode());
		authLog.setAuthCode(defaultApprovalConfig.getAuthCode());
		authLog.setGpasResponseDescription(defaultApprovalConfig.getDescriptionText());
		authLog.setVendorInfoBlock(defaultApprovalConfig.getVendorInfo());
		authLog.setMessageStatus(MessageStatus.Successful);
		authLog.setDefaultResponse(true);

		authLogRepo.update(authLog);

		return authLog;
	}
}