package com.staples.payment.authorization.service.factory;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.shared.bambora.request.SaleRequest;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.MerchantMaster;

public interface BamboraRequestFactory
{
	SaleRequest createSaleRequest(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog);
}