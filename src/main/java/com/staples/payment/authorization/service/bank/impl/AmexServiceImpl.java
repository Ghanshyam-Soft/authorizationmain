package com.staples.payment.authorization.service.bank.impl;

import org.springframework.stereotype.Service;

import com.staples.payment.authorization.bank.response.AmexPWPBalanceResponse;
import com.staples.payment.authorization.clients.AmexPWPClient;
import com.staples.payment.authorization.configuration.properties.EnabledFeatures;
import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.details.AuthReqAmexPWP;
import com.staples.payment.authorization.service.bank.AmexService;
import com.staples.payment.authorization.service.factory.AmexFactory;
import com.staples.payment.shared.amexpwp.request.GetBalanceRequest;
import com.staples.payment.shared.amexpwp.request.PayPointsRequest;
import com.staples.payment.shared.amexpwp.response.AmexGetBalanceResponse;
import com.staples.payment.shared.amexpwp.response.AmexResponseWrapper;
import com.staples.payment.shared.amexpwp.response.PayWithRewardsResponse;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.bank.AmexBalanceResponse;
import com.staples.payment.shared.entity.bank.AmexRedeemResponse;
import com.staples.payment.shared.repo.bank.AmexBalanceResponseRepo;
import com.staples.payment.shared.repo.bank.AmexRedeemResponseRepo;

@Service
public class AmexServiceImpl implements AmexService
{
	private final AmexPWPClient amexPWPClient;
	private final AmexFactory amexFactory;
	private final AmexRedeemResponseRepo pwpRedeemPointsRepo;
	private final AmexBalanceResponseRepo pwpGetBalanceRepo;
	private final EnabledFeatures enabledFeatures;

	public AmexServiceImpl(AmexPWPClient amexPWPClient, AmexFactory amexFactory, AmexRedeemResponseRepo pwpRedeemPointsRepo, AmexBalanceResponseRepo pwpGetBalanceRepo, EnabledFeatures enabledFeatures)
	{
		this.amexPWPClient = amexPWPClient;
		this.amexFactory = amexFactory;
		this.pwpRedeemPointsRepo = pwpRedeemPointsRepo;
		this.pwpGetBalanceRepo = pwpGetBalanceRepo;
		this.enabledFeatures = enabledFeatures;
	}

	@Override
	public AmexPWPBalanceResponse getBalance(GetBalanceRequest request)
	{
		if(!enabledFeatures.isPwpBalance())
		{
			throw new UnsupportedOperationException("AmexPwp getBalance is not yet implemented");
		}

		AmexBalanceResponse responseEnitity = saveInitialRequest(request);
		return processBalanceRequest(request, responseEnitity);
	}

	private AmexPWPBalanceResponse processBalanceRequest(GetBalanceRequest request, AmexBalanceResponse responseEntity)
	{
		AmexResponseWrapper<AmexGetBalanceResponse> response = amexPWPClient.getRewardsBalance(request);

		responseEntity = amexFactory.updateBalanceResponse(response, responseEntity);

		pwpGetBalanceRepo.update(responseEntity);

		return amexFactory.createAmexPwpResponse(response, responseEntity);
	}

	private AmexBalanceResponse saveInitialRequest(GetBalanceRequest request)
	{
		String messageId = request.getMessageId();

		this.duplicateRequestCheckForBalance(messageId);

		AmexBalanceResponse responseEntity = amexFactory.createInitialBalanceResponse(request);
		pwpGetBalanceRepo.insert(responseEntity);

		return responseEntity;
	}

	private void duplicateRequestCheckForBalance(String messageId)
	{
		boolean authLogExists = pwpGetBalanceRepo.existsById(messageId);
		if(authLogExists)
		{
			throw new InvalidInputException("Message Id already exists.");
		}
	}

	private void duplicateRequestCheckForRedeem(String messageId)
	{
		boolean authLogExists = pwpRedeemPointsRepo.existsById(messageId);
		if(authLogExists)
		{
			throw new InvalidInputException("Message Id already exists.");
		}
	}

	@Override
	public AuthLog processPayPoints(AuthLog authLog, AuthRequest authRequest)
	{
		AuthReqAmexPWP requestAmex = authRequest.getAmexPWP();
		String authCode = authLog.getAuthCode();
		AuthRequestType requestType = authRequest.getTransactionHeader().getRequestType();

		if(requestAmex != null && requestAmex.getRewardsAmount() != null && authCode != null
				&& requestType == AuthRequestType.Authorization && enabledFeatures.isPwpRedeem())
		{
			String paymentToken = authRequest.getTransactionDetail().getPaymentToken();
			String gpasKey = authLog.getGpasKey();

			final PayPointsRequest payPointRequest = amexFactory.createPayPointsRequest(requestAmex, gpasKey, authCode, paymentToken);

			AmexRedeemResponse responseEntity = saveInitialRequest(authLog, payPointRequest);

			authLog = processPayPointsRequest(authLog, payPointRequest, responseEntity);
		}

		return authLog;
	}

	private AuthLog processPayPointsRequest(AuthLog authLog, PayPointsRequest payPointRequest, AmexRedeemResponse amexRedeemResponse)
	{
		AmexResponseWrapper<PayWithRewardsResponse> response = amexPWPClient.payPoints(payPointRequest);

		amexRedeemResponse = amexFactory.updateRedeemPointsResponse(response, amexRedeemResponse);

		authLog.setAmexRedeemResponse(amexRedeemResponse);
		pwpRedeemPointsRepo.update(amexRedeemResponse);

		return authLog;
	}

	private AmexRedeemResponse saveInitialRequest(AuthLog authLog, PayPointsRequest request)
	{
		String messageId = request.getMessageId();

		this.duplicateRequestCheckForRedeem(messageId);

		AmexRedeemResponse responseEntity = amexFactory.createInitialRedeemResponse(request);

		insertInitialAmexPwpResponse(authLog, responseEntity);

		return responseEntity;
	}

	private void insertInitialAmexPwpResponse(final AuthLog authLog, AmexRedeemResponse responseEntity)
	{
		authLog.setAmexRedeemResponse(responseEntity);

		pwpRedeemPointsRepo.insert(responseEntity);
	}
}