package com.staples.payment.authorization.service.bank;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.MerchantMaster;

public interface AciPaymentService
{
	AuthLog process(AuthLog authLog, AuthRequest gpasRequest, MerchantMaster merchantMaster);
}