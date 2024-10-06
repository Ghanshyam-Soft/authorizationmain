package com.staples.payment.authorization.validation;

import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;

public interface RequestBeanValidation
{
	void validateSpecificFields(AuthRequest request) throws InvalidInputException;
}