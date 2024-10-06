package com.staples.payment.authorization.service.impl;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.response.AuthResponse;
import com.staples.payment.authorization.service.LookupService;
import com.staples.payment.authorization.service.factory.AuthLogFactory;
import com.staples.payment.authorization.service.factory.ResponseFactory;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.GpasRespCode;
import com.staples.payment.shared.constant.MessageStatus;
import com.staples.payment.shared.constant.PaymentType;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.repo.AuthLogRepo;
import com.staples.payment.shared.repo.bank.BraintreeResponseRepo;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LookupServiceImpl implements LookupService
{
	private final AuthLogRepo authLogRepo;
	private final BraintreeResponseRepo braintreeResponseRepo;
	private final AuthLogFactory authLogFactory;
	private final ResponseFactory responseFactory;

	public LookupServiceImpl(AuthLogRepo authLogRepo, BraintreeResponseRepo braintreeResponseRepo, AuthLogFactory authLogFactory, ResponseFactory responseFactory)
	{
		super();
		this.authLogRepo = authLogRepo;
		this.braintreeResponseRepo = braintreeResponseRepo;
		this.authLogFactory = authLogFactory;
		this.responseFactory = responseFactory;
	}

	@Override
	public AuthResponse processLookupRequest(final AuthRequest request, Instant receivedTime)
	{
		val transactionHeader = request.getTransactionHeader();
		val originatingGuid = transactionHeader.getOriginatingGUID();

		Optional<AuthLog> originalAuthLogOptional = authLogRepo.findById(originatingGuid); // the OriginatingGUID is the ChildGUID of the original transaction

		if(originalAuthLogOptional.isPresent())
		{
			AuthLog originalAuthLog = originalAuthLogOptional.get();

			AuthLog lookupAuthLog = authLogFactory.createDuplicateAuthLog(request, receivedTime, originalAuthLog);
			authLogRepo.insert(lookupAuthLog);

			lookupAuthLog = verifyResponseExists(originalAuthLog, request, lookupAuthLog);

			return responseFactory.createAuthResponse(lookupAuthLog, request);
		}
		else
		{
			AuthLog lookupAuthLog = insertErrorAuthLog(request, receivedTime);
			return responseFactory.createAuthResponse(lookupAuthLog, request);
		}
	}

	private AuthLog verifyResponseExists(AuthLog originalAuthLog, AuthRequest request, AuthLog lookupAuthLog)
	{
		PaymentType paymentType = request.getTransactionHeader().getPaymentType();
		PaymentType originalPaymentType = originalAuthLog.getPaymentType();
		AuthRequestType originalRequestType = originalAuthLog.getRequestType();

		if(originalRequestType == AuthRequestType.Authorization)
		{
			if(paymentType == originalPaymentType)
			{
				String gpasKey = originalAuthLog.getGpasKey();

				boolean responseExists = braintreeResponseRepo.existsById(gpasKey);

				if(!responseExists)
				{
					log.info("original auth response is not present in braintree response table for Lookup ChildGUID {} and Original gpasKey {}", lookupAuthLog.getChildKey(), gpasKey);
					lookupAuthLog = updateAuthLogStatusForError(lookupAuthLog);
				}

				return lookupAuthLog;
			}
			else
			{
				throw new RuntimeException("Invalid payment type for Lookup");
			}
		}
		else
		{
			throw new InvalidInputException("Request Type does not match for Lookup");
		}
	}

	private AuthLog updateAuthLogStatusForError(AuthLog lookupAuthLog)
	{
		log.info("Updating message status to System issue : guid {}", lookupAuthLog.getChildKey());

		lookupAuthLog.setGpasReasCode("30");
		lookupAuthLog.setGpasRespCode(GpasRespCode.D);
		lookupAuthLog.setMessageStatus(MessageStatus.System_Issue);

		authLogRepo.save(lookupAuthLog);

		return lookupAuthLog;
	}

	private AuthLog insertErrorAuthLog(final AuthRequest request, Instant receivedTime)
	{
		AuthLog authLog = authLogFactory.createAuthLog(request, receivedTime);

		authLog.setGpasReasCode("30");
		authLog.setGpasRespCode(GpasRespCode.D);
		authLog.setMessageStatus(MessageStatus.System_Issue);

		authLogRepo.insert(authLog);

		return authLog;
	}
}