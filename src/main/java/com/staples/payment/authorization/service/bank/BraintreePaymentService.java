package com.staples.payment.authorization.service.bank;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.shared.entity.AuthLog;

public interface BraintreePaymentService
{
	AuthLog process(AuthLog authLog, AuthRequest gpasRequest);
}