package com.staples.payment.authorization.service.factory;

import java.time.Instant;

import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.shared.entity.AuthLog;

public interface AuthLogFactory
{
	AuthLog createAuthLog(AuthRequest request, Instant receivedTime);

	AuthLog createDuplicateAuthLog(AuthRequest request, Instant receivedTime, AuthLog existingAuthLog) throws InvalidInputException;
}
