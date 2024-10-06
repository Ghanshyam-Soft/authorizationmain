package com.staples.payment.authorization.service;

import java.time.Instant;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.response.AuthResponse;

public interface LookupService
{
	AuthResponse processLookupRequest(AuthRequest request, Instant receivedTime);
}