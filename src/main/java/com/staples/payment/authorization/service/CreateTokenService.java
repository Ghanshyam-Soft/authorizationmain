package com.staples.payment.authorization.service;

import com.staples.payment.authorization.request.CreateTokenRequest;
import com.staples.payment.authorization.response.CreateTokenResponse;

public interface CreateTokenService
{
	CreateTokenResponse processCreateTokenRequest(CreateTokenRequest request, String internalRequestId);
}
