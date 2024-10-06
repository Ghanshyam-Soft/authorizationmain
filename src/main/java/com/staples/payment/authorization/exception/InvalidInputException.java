package com.staples.payment.authorization.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidInputException extends RuntimeException
{
	private static final long serialVersionUID = -293286599637516525L;

	public InvalidInputException()
	{
		super();
	}

	public InvalidInputException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public InvalidInputException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public InvalidInputException(String message)
	{
		super(message);
	}

	public InvalidInputException(Throwable cause)
	{
		super(cause);
	}
}