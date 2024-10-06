package com.staples.payment.authorization.service.factory;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.shared.braintree.request.SaleRequest;
import com.staples.payment.shared.braintree.request.VoidRequest;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.braintree.BraintreeResponse;

public interface BraintreeRequestFactory
{
	SaleRequest createSaleRequest(AuthRequest gpasRequest, AuthLog authLog);

	VoidRequest createVoidRequest(BraintreeResponse braintreeResponse);
}