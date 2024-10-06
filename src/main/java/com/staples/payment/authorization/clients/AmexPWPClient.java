package com.staples.payment.authorization.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.staples.payment.authorization.configuration.feign.AmexPWPFeignClientConfiguration;
import com.staples.payment.shared.amexpwp.request.GetBalanceRequest;
import com.staples.payment.shared.amexpwp.request.PayPointsRequest;
import com.staples.payment.shared.amexpwp.response.AmexGetBalanceResponse;
import com.staples.payment.shared.amexpwp.response.AmexResponseWrapper;
import com.staples.payment.shared.amexpwp.response.PayWithRewardsResponse;

@FeignClient(name = "amex-pwp-client", url = "${amexpwp.service.baseUrl}", path = "/rewards", configuration = AmexPWPFeignClientConfiguration.class)
public interface AmexPWPClient
{
	@PostMapping(value = "/paypoints")
	AmexResponseWrapper<PayWithRewardsResponse> payPoints(PayPointsRequest request);

	@PostMapping(value = "/getbalance")
	AmexResponseWrapper<AmexGetBalanceResponse> getRewardsBalance(GetBalanceRequest amexGetBalanceRequest);
}