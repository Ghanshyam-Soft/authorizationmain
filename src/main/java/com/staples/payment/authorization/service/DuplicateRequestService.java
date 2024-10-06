package com.staples.payment.authorization.service;

import java.time.Instant;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.response.AuthResponse;

public interface DuplicateRequestService
{
	void childKeyDuplicateCheck(AuthRequest request);

	AuthResponse origKeyDuplicateCheck(AuthRequest request, Instant receivedTime);
}
