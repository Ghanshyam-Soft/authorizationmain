package com.staples.payment.authorization.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.staples.payment.authorization.exception.BankRespNotRecievedException;
import com.staples.payment.authorization.exception.DuplicateChildException;
import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.response.AuthResponse;
import com.staples.payment.authorization.service.DuplicateRequestService;
import com.staples.payment.authorization.service.factory.AuthLogFactory;
import com.staples.payment.authorization.service.factory.ResponseFactory;
import com.staples.payment.shared.constant.MessageStatus;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.repo.AuthLogRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DuplicateRequestServiceImpl implements DuplicateRequestService
{
	private final ResponseFactory responseFactory;
	private final AuthLogFactory authLogFactory;
	private final AuthLogRepo authLogRepo;

	public DuplicateRequestServiceImpl(AuthLogFactory authLogFactory, AuthLogRepo authLogRepo, ResponseFactory responseFactory)
	{
		super();
		this.responseFactory = responseFactory;
		this.authLogFactory = authLogFactory;
		this.authLogRepo = authLogRepo;
	}

	@Override
	public void childKeyDuplicateCheck(final AuthRequest request)
	{
		final String childGuid = request.getTransactionHeader().getChildGUID();

		log.info("Duplicate check started for Child GUID : {}", childGuid);

		boolean authLogExists = authLogRepo.existsById(childGuid);
		if(authLogExists)
		{
			throw new DuplicateChildException("Child Key already exists.");
		}
	}

	@Override
	public @Nullable AuthResponse origKeyDuplicateCheck(final AuthRequest request, Instant receivedTime) throws InvalidInputException, BankRespNotRecievedException
	{
		final String childGuid = request.getTransactionHeader().getChildGUID();
		final String origGuid = request.getTransactionHeader().getOriginatingGUID();

		AuthLog existingAuthLog = getExistingAuthLogBy(origGuid);

		if(existingAuthLog != null)
		{
			log.debug("Child GUID = {} Duplicate found for orig GUId={}. ChildKey of existingAuthLog is {}", childGuid, origGuid, existingAuthLog.getChildKey());

			insertDuplicateAuthLog(request, receivedTime, existingAuthLog);

			return createDuplicateResponse(request, existingAuthLog);
		}
		return null;
	}

	private @Nullable AuthLog getExistingAuthLogBy(final String origGuid) throws InvalidInputException, BankRespNotRecievedException
	{
		// This duplication check checks only by the originating key, the earlier validation checks by child key.

		if(!StringUtils.hasText(origGuid))
		{
			return null;
		}
		else
		{
			List<AuthLog> existingAuthLogs = authLogRepo.findByChildKeyOrOriginatingKey(origGuid, origGuid); // if the originating guid matches an existing originating guid or child key

			if(existingAuthLogs == null || existingAuthLogs.isEmpty())
			{
				return null;
			}
			else
			{
				boolean finishedProcessing = areAnyFinishedProcessing(existingAuthLogs);
				if(!finishedProcessing)
				{
					throw new BankRespNotRecievedException("Bank communication not yet complete for original request.");
				}

				AuthLog existingAuthLog = getExistingAuthLogIfSuccessOrDuplicate(existingAuthLogs);
				return existingAuthLog;
			}
		}
	}

	private boolean areAnyFinishedProcessing(List<AuthLog> existingAuthLogs)
	{
		for(AuthLog existingAuthLog : existingAuthLogs)
		{
			if(existingAuthLog.getMessageStatus() != null)
			{
				return true;
			}
		}
		return false;
	}

	private @Nullable AuthLog getExistingAuthLogIfSuccessOrDuplicate(List<AuthLog> existingAuthLogs)
	{
		for(AuthLog existingAuthLog : existingAuthLogs)
		{
			final MessageStatus messageStatus = existingAuthLog.getMessageStatus();
			if(messageStatus == MessageStatus.Successful || messageStatus == MessageStatus.Duplicate)
			{
				return existingAuthLog;
			}
		}
		return null; // if none of the existing authLogs are successful or duplicate, then will continue processing this request as if not a duplicate
	}

	private AuthLog insertDuplicateAuthLog(final AuthRequest request, Instant receivedTime, AuthLog existingAuthLog) throws InvalidInputException
	{
		AuthLog authLog = authLogFactory.createDuplicateAuthLog(request, receivedTime, existingAuthLog);

		authLogRepo.insert(authLog);

		return authLog;
	}

	private AuthResponse createDuplicateResponse(AuthRequest request, AuthLog existingAuthLog)
	{
		AuthResponse authResponse = responseFactory.createAuthResponse(existingAuthLog, request);

		authResponse.getTransactionDetail().setMessageStatus(MessageStatus.Duplicate);

		return authResponse;
	}
}