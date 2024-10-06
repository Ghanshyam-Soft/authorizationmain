package com.staples.payment.authorization.service;

import java.time.Instant;

import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.response.AuthResponse;
import com.staples.payment.shared.exceptions.CustomException;

public interface PaymentProcessingService
{
	AuthResponse processAuthRequest(AuthRequest request, Instant receivedTime) throws InvalidInputException, CustomException;

	void updateAuthLogStatusForError(String childGUID);
}