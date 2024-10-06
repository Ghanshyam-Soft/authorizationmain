package com.staples.payment.authorization.service.factory.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.staples.payment.authorization.bank.response.AmexPWPAccountKey;
import com.staples.payment.authorization.bank.response.AmexPWPBalanceResponse;
import com.staples.payment.authorization.bank.response.AmexPWPEmbedded;
import com.staples.payment.authorization.bank.response.AmexPWPPayAmount;
import com.staples.payment.authorization.bank.response.AmexResponseStatus;
import com.staples.payment.authorization.request.details.AuthReqAmexPWP;
import com.staples.payment.authorization.service.factory.AmexFactory;
import com.staples.payment.shared.amexpwp.request.GetBalanceRequest;
import com.staples.payment.shared.amexpwp.request.PayPointsRequest;
import com.staples.payment.shared.amexpwp.response.AmexGetBalanceResponse;
import com.staples.payment.shared.amexpwp.response.AmexResponseWrapper;
import com.staples.payment.shared.amexpwp.response.PayWithRewardsResponse;
import com.staples.payment.shared.amexpwp.response.details.FailureDetails;
import com.staples.payment.shared.cache.ResponseInfoCache;
import com.staples.payment.shared.constant.Bank;
import com.staples.payment.shared.constant.GpasRespCode;
import com.staples.payment.shared.entity.bank.AmexBalanceResponse;
import com.staples.payment.shared.entity.bank.AmexRedeemResponse;
import com.staples.payment.shared.entity.respInfo.ReasonResponseInfo;
import com.staples.payment.shared.entity.respInfo.RespResponseInfo;

import lombok.val;

@Service
public class AmexFactoryImpl implements AmexFactory
{
	private final ResponseInfoCache responseInfoCache;
	private final ObjectMapper objectMapper;
	private final Bank bankId = Bank.AMXPWP;

	public AmexFactoryImpl(ResponseInfoCache responseInfoCache, ObjectMapper objectMapper)
	{
		super();
		this.responseInfoCache = responseInfoCache;
		this.objectMapper = objectMapper;
	}

	@Override
	public PayPointsRequest createPayPointsRequest(AuthReqAmexPWP request, String gpasKey, String authCode, String paymentToken)
	{
		return PayPointsRequest.builder()
				.pointsNeeded(request.getRewardsPointsRedeemed())
				.gpasKey(gpasKey)
				.amount(request.getCurrencyAmount())
				.paymentToken(paymentToken)
				.messageId(request.getRequestID())
				.basketAmount(request.getRewardsAmount())
				.chargeId(authCode)
				.build();
	}

	@Override
	public AmexBalanceResponse createInitialBalanceResponse(GetBalanceRequest request)
	{
		var pwpResponse = AmexBalanceResponse.builder()
				.messageId(request.getMessageId())
				.paymentToken(request.getPaymentToken())
				.reqReceiveDatetime(Instant.now());

		return pwpResponse.build();
	}

	@Override
	public AmexRedeemResponse createInitialRedeemResponse(PayPointsRequest request)
	{
		var redeemResponse = AmexRedeemResponse.builder()
				.messageId(request.getMessageId())
				.paymentToken(request.getPaymentToken())
				.chargeId(request.getChargeId())
				.gpasKey(request.getGpasKey())
				.reqReceiveDatetime(Instant.now());

		return redeemResponse.build();
	}

	@Override
	public AmexRedeemResponse updateRedeemPointsResponse(AmexResponseWrapper<PayWithRewardsResponse> response, AmexRedeemResponse responseEntity)
	{
		RespResponseInfo responseInfoResp = null;
		ReasonResponseInfo responseInfoReas = null;
		List<FailureDetails> failureList = null;

		if(response.getStatusCode().is2xxSuccessful())
		{
			responseInfoResp = responseInfoCache.getRespResponseInfoBy(bankId, null, null, "Success");
			responseInfoReas = responseInfoCache.getReasonResponseInfoBy(bankId, null, null, "Success");
		}
		else
		{
			failureList = response.getResponseBody().getFailureList();

			FailureDetails errorList = failureList.get(0);

			String errorCode = errorList.getErrorCode();

			if(!StringUtils.hasText(errorCode))
			{
				errorCode = errorList.getApplicationErrorCode();

				if(!StringUtils.hasText(errorCode))
				{
					throw new RuntimeException("No error code is recieved for the failure.");
				}
			}

			responseInfoResp = responseInfoCache.getRespResponseInfoBy(bankId, errorCode, null, errorList.getUserMessage());
			responseInfoReas = responseInfoCache.getReasonResponseInfoBy(bankId, errorCode, null, errorList.getUserMessage());
		}

		var payWithRewardsResponse = response.getResponseBody();
		var amountData = payWithRewardsResponse.getAmount();
		var basketAmountData = payWithRewardsResponse.getBasketAmount();
		var embedded = payWithRewardsResponse.getEmbedded();

		if(amountData != null)
		{
			responseEntity.setAmount(amountData.getValue());
			responseEntity.setAmountCurrencyCode(amountData.getCurrencyCode());
		}

		if(basketAmountData != null)
		{
			responseEntity.setBasketAmount(basketAmountData.getValue());
			responseEntity.setBasketAmountCurrencyCode(basketAmountData.getCurrencyCode());
		}
		if(embedded != null)
		{
			responseEntity.setCurrentBalance(embedded.getCurrentBalance());
			responseEntity.setAmountBalance(embedded.getAmountBalance());
			responseEntity.setTierCode(embedded.getTierCode());
		}

		responseEntity.setConversionRate(payWithRewardsResponse.getConversionRate());
		responseEntity.setPointsNeeded(payWithRewardsResponse.getPointsNeeded());
		responseEntity.setReqSentTime(response.getTimeSent());
		responseEntity.setResRcvdTime(response.getTimeReceived());
		responseEntity.setStatus(response.getStatusCode().value());

		responseEntity.setResponseCode(responseInfoResp.getGpasCode());
		responseEntity.setReasonCode(responseInfoReas.getGpasCode());
		responseEntity.setVendorResponseCode(responseInfoResp.getBankCode());
		responseEntity.setVendorReasonCode(responseInfoReas.getBankCode());

		final String gpasResponseDesc = responseInfoResp.getGpasCodeDesc() + " " + responseInfoReas.getGpasCodeDesc();

		responseEntity.setDescriptionText(gpasResponseDesc);
		responseEntity.setErrorDescription(createReadableErrorList(failureList));

		return responseEntity;
	}

	@Override
	public AmexBalanceResponse updateBalanceResponse(AmexResponseWrapper<AmexGetBalanceResponse> response, AmexBalanceResponse responseEntity)
	{
		RespResponseInfo responseInfoResp = null;
		ReasonResponseInfo responseInfoReas = null;
		List<FailureDetails> failureList = null;

		if(response.getStatusCode().is2xxSuccessful())
		{
			responseInfoResp = responseInfoCache.getRespResponseInfoBy(bankId, null, null, "Success");
			responseInfoReas = responseInfoCache.getReasonResponseInfoBy(bankId, null, null, "Success");
		}
		else
		{
			failureList = response.getResponseBody().getFailureList();

			FailureDetails errorList = failureList.get(0);

			String errorCode = errorList.getErrorCode();

			if(!StringUtils.hasText(errorCode))
			{
				errorCode = errorList.getApplicationErrorCode();

				if(!StringUtils.hasText(errorCode))
				{
					throw new RuntimeException("No error code is recieved for the failure.");
				}
			}

			responseInfoResp = responseInfoCache.getRespResponseInfoBy(bankId, errorCode, null, errorList.getUserMessage());
			responseInfoReas = responseInfoCache.getReasonResponseInfoBy(bankId, errorCode, null, errorList.getUserMessage());
		}

		val balanceResponse = response.getResponseBody();
		val amountData = balanceResponse.getAmount();
		val embedded = balanceResponse.getEmbedded();
		val accountKey = balanceResponse.getAccountKey();

		responseEntity.setRequestorOrderId(balanceResponse.getRequestorOrderId());
		responseEntity.setConversionRate(balanceResponse.getConversionRate());

		if(amountData != null)
		{
			responseEntity.setAmount(amountData.getValue());
			responseEntity.setAmountCurrencyCode(amountData.getCurrencyCode());
		}

		if(embedded != null)
		{
			responseEntity.setCurrentBalance(embedded.getCurrentBalance());
			responseEntity.setAmountBalance(embedded.getAmountBalance());
			responseEntity.setTierCode(embedded.getTierCode());
		}

		if(accountKey != null)
		{
			responseEntity.setAccountId(accountKey.getAccountId());
		}

		responseEntity.setReqSentTime(response.getTimeSent());
		responseEntity.setResRcvdTime(response.getTimeReceived());
		responseEntity.setStatus(response.getStatusCode().value());

		responseEntity.setResponseCode(responseInfoResp.getGpasCode());
		responseEntity.setReasonCode(responseInfoReas.getGpasCode());
		responseEntity.setVendorResponseCode(responseInfoResp.getBankCode());
		responseEntity.setVendorReasonCode(responseInfoReas.getBankCode());

		final String gpasResponseDesc = responseInfoResp.getGpasCodeDesc() + " " + responseInfoReas.getGpasCodeDesc();

		responseEntity.setDescriptionText(gpasResponseDesc);
		responseEntity.setErrorDescription(createReadableErrorList(failureList));

		return responseEntity;
	}

	@Override
	public AmexPWPBalanceResponse createAmexPwpResponse(AmexResponseWrapper<AmexGetBalanceResponse> response, AmexBalanceResponse finalResponse)
	{
		AmexPWPAccountKey accountKey = null;
		AmexPWPPayAmount amount = null;
		AmexPWPEmbedded embedded = null;
		List<FailureDetails> failureList = null;
		String vendorDescription = null;

		failureList = response.getResponseBody().getFailureList();

		if(failureList != null && failureList.size() > 0)
		{
			vendorDescription = failureList.get(0).getUserMessage();
		}

		if(finalResponse.getResponseCode() != null && finalResponse.getResponseCode().equals(GpasRespCode.A))
		{
			accountKey = AmexPWPAccountKey.builder()
					.accountId(finalResponse.getAccountId())
					.build();

			amount = AmexPWPPayAmount.builder()
					.value(finalResponse.getAmount())
					.currencyCode(finalResponse.getAmountCurrencyCode())
					.build();

			embedded = AmexPWPEmbedded.builder()
					.currentBalance(finalResponse.getCurrentBalance())
					.amountBalance(finalResponse.getAmountBalance())
					.tierCode(finalResponse.getTierCode())
					.build();
		}

		AmexResponseStatus responseStatus = AmexResponseStatus.builder()
				.responseCode(finalResponse.getResponseCode())
				.reasonCode(finalResponse.getReasonCode())
				.vendorResponseCode(finalResponse.getVendorResponseCode())
				.vendorReasonCode(finalResponse.getVendorReasonCode())
				.descriptionText(finalResponse.getDescriptionText())
				.vendorDescription(vendorDescription)
				.build();

		return AmexPWPBalanceResponse.builder()
				.requestorOrderId(finalResponse.getRequestorOrderId())
				.accountKey(accountKey)
				.amount(amount)
				.conversionRate(finalResponse.getConversionRate())
				.embedded(embedded)
				.responseStatus(responseStatus)
				.build();
	}

	public String createReadableErrorList(List<FailureDetails> errorList)
	{
		if(errorList == null || errorList.size() <= 0)
		{
			return null;
		}

		String errorsJson;
		try
		{
			errorsJson = objectMapper.writeValueAsString(errorList);
		}
		catch(JsonProcessingException e)
		{
			throw new RuntimeException("Failed to parse error response from json for AmexPwp transaction : ", e);
		}

		return errorsJson;
	}
}
