package com.staples.payment.authorization.service.bank;

import com.staples.payment.authorization.bank.response.AmexPWPBalanceResponse;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.shared.amexpwp.request.GetBalanceRequest;
import com.staples.payment.shared.entity.AuthLog;

public interface AmexService
{
	AmexPWPBalanceResponse getBalance(GetBalanceRequest request);

	AuthLog processPayPoints(AuthLog authLog, AuthRequest authRequest);
}