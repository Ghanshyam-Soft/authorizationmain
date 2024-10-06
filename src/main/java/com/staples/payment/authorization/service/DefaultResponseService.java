package com.staples.payment.authorization.service;

import org.springframework.lang.Nullable;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.response.AuthResponse;
import com.staples.payment.shared.entity.AuthLog;

public interface DefaultResponseService
{
	@Nullable
	AuthResponse createDefaultResponseIfNeeded(AuthRequest request, AuthLog authLog);
}
