package com.staples.payment.authorization.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.staples.payment.shared.exceptions.CustomException;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class BankServiceException extends CustomException
{
	private static final long serialVersionUID = 1L;

	public BankServiceException(String message, Exception e)
	{
		super(message, e);
	}
}
