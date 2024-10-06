package com.staples.payment.authorization.service;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.shared.entity.AuthLog;

public interface ThreeDSService
{
	AuthLog set3dsProperties(AuthLog authLog, AuthRequest request);
}