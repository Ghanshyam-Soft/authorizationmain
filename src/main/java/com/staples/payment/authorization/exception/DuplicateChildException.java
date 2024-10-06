package com.staples.payment.authorization.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.staples.payment.shared.exceptions.CustomException;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY) // best fitted HTTP status code here
public class DuplicateChildException extends CustomException
{
	private static final long serialVersionUID = 1L;

	public DuplicateChildException(String message)
	{
		super(message);
	}
}
