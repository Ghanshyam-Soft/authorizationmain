package com.staples.payment.authorization.service.factory;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.response.AuthResponse;
import com.staples.payment.shared.entity.AuthLog;

public interface ResponseFactory
{
	AuthResponse createAuthResponse(AuthLog authLog, AuthRequest request);
}
