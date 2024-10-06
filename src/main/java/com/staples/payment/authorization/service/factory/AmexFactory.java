package com.staples.payment.authorization.service.factory;

import com.staples.payment.authorization.bank.response.AmexPWPBalanceResponse;
import com.staples.payment.authorization.request.details.AuthReqAmexPWP;
import com.staples.payment.shared.amexpwp.request.GetBalanceRequest;
import com.staples.payment.shared.amexpwp.request.PayPointsRequest;
import com.staples.payment.shared.amexpwp.response.AmexGetBalanceResponse;
import com.staples.payment.shared.amexpwp.response.AmexResponseWrapper;
import com.staples.payment.shared.amexpwp.response.PayWithRewardsResponse;
import com.staples.payment.shared.entity.bank.AmexBalanceResponse;
import com.staples.payment.shared.entity.bank.AmexRedeemResponse;

public interface AmexFactory
{
	PayPointsRequest createPayPointsRequest(AuthReqAmexPWP request, String gpasKey, String authCode, String paymentToken);

	AmexBalanceResponse createInitialBalanceResponse(GetBalanceRequest request);

	AmexRedeemResponse createInitialRedeemResponse(PayPointsRequest request);

	AmexRedeemResponse updateRedeemPointsResponse(AmexResponseWrapper<PayWithRewardsResponse> response, AmexRedeemResponse responseEntity);

	AmexBalanceResponse updateBalanceResponse(AmexResponseWrapper<AmexGetBalanceResponse> response, AmexBalanceResponse responseEntity);

	AmexPWPBalanceResponse createAmexPwpResponse(AmexResponseWrapper<AmexGetBalanceResponse> response, AmexBalanceResponse finalResponse);

}