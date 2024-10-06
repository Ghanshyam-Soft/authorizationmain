package com.staples.payment.authorization.service.impl;

import java.time.Instant;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.stereotype.Service;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.response.AuthResponse;
import com.staples.payment.authorization.service.DefaultResponseService;
import com.staples.payment.authorization.service.DuplicateRequestService;
import com.staples.payment.authorization.service.LookupService;
import com.staples.payment.authorization.service.PaymentProcessingService;
import com.staples.payment.authorization.service.ThreeDSService;
import com.staples.payment.authorization.service.VendorRouter;
import com.staples.payment.authorization.service.factory.AuthLogFactory;
import com.staples.payment.authorization.service.factory.ResponseFactory;
import com.staples.payment.authorization.validation.RequestBeanValidation;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.MessageStatus;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.repo.AuthLogRepo;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentProcessingServiceImpl implements PaymentProcessingService
{
	private final RequestBeanValidation beanValidation;
	private final AuthLogFactory authLogFactory;
	private final ResponseFactory responseFactory;
	private final AuthLogRepo authLogRepo;
	private final VendorRouter vendorRouter;
	private final ThreeDSService threeDSService;
	private final LookupService lookupService;
	private final DefaultResponseService defaultResponseService;
	private final DuplicateRequestService duplicateRequestService;

	public PaymentProcessingServiceImpl(RequestBeanValidation beanValidation, AuthLogFactory authLogFactory, AuthLogRepo authLogRepo, VendorRouter vendorRouter,
			LookupService lookupService, DefaultResponseService defaultResponseService, ThreeDSService threeDSService, DuplicateRequestService duplicateRequestService, ResponseFactory responseFactory)
	{
		this.beanValidation = beanValidation;
		this.authLogFactory = authLogFactory;
		this.responseFactory = responseFactory;
		this.authLogRepo = authLogRepo;
		this.vendorRouter = vendorRouter;
		this.threeDSService = threeDSService;
		this.lookupService = lookupService;
		this.defaultResponseService = defaultResponseService;
		this.duplicateRequestService = duplicateRequestService;
	}

	@Override
	public AuthResponse processAuthRequest(final AuthRequest request, Instant receivedTime)
	{
		val transactionHeader = request.getTransactionHeader();

		this.validateAuthRequest(request);

		if(AuthRequestType.Lookup == transactionHeader.getRequestType())
		{
			return lookupService.processLookupRequest(request, receivedTime);
		}
		else
		{
			return processPaymentRequest(request, receivedTime);
		}
	}

	private void validateAuthRequest(@Valid final AuthRequest request)
	{
		duplicateRequestService.childKeyDuplicateCheck(request);

		beanValidation.validateSpecificFields(request);
	}

	private AuthResponse processPaymentRequest(final AuthRequest request, Instant receivedTime)
	{
		val transactionHeader = request.getTransactionHeader();
		val childGuid = transactionHeader.getChildGUID();

		// Check for duplicate and send response if duplicate..bank call not required for duplicate
		AuthResponse duplicateResponse = duplicateRequestService.origKeyDuplicateCheck(request, receivedTime);
		if(duplicateResponse != null)
		{
			log.info("Duplicate Response for childguid {} : {} ", childGuid, duplicateResponse);
			return duplicateResponse;
		}

		AuthLog authLog = insertAuthLog(request, receivedTime);
		authLog = threeDSService.set3dsProperties(authLog, request);

		AuthResponse defaultResponse = defaultResponseService.createDefaultResponseIfNeeded(request, authLog);
		if(defaultResponse != null)
		{
			log.info("Default Response for childguid {} : {} ", childGuid, defaultResponse);
			return defaultResponse;
		}

		final AuthLog returnedAuthLog = vendorRouter.routePayment(request, authLog);

		return responseFactory.createAuthResponse(returnedAuthLog, request);
	}

	@Override
	public void updateAuthLogStatusForError(String childGUID)
	{
		Optional<AuthLog> authLogOptional = authLogRepo.findById(childGUID);
		if(authLogOptional.isPresent())
		{
			AuthLog savedAuthLog = authLogOptional.get();

			if(savedAuthLog.getMessageStatus() == null)
			{
				log.info("Updating message status to System issue : Child GUID {}", childGUID);
				savedAuthLog.setMessageStatus(MessageStatus.System_Issue);
				authLogRepo.update(savedAuthLog);
			}
		}
	}

	private AuthLog insertAuthLog(final AuthRequest request, Instant receivedTime)
	{
		AuthLog authLog = authLogFactory.createAuthLog(request, receivedTime);
		authLogRepo.insert(authLog);

		return authLog;
	}
}