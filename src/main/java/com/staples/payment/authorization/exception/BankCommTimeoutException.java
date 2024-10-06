package com.staples.payment.authorization.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.staples.payment.shared.exceptions.CustomException;

@ResponseStatus(value = HttpStatus.GATEWAY_TIMEOUT)
public class BankCommTimeoutException extends CustomException
{
	private static final long serialVersionUID = 1L;

	public BankCommTimeoutException(String message)
	{
		super(message);
	}

	public BankCommTimeoutException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
