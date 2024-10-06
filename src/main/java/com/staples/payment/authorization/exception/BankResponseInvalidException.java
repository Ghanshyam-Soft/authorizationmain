package com.staples.payment.authorization.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.staples.payment.shared.exceptions.CustomException;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class BankResponseInvalidException extends CustomException
{
	private static final long serialVersionUID = -6481805721993432956L;

	public BankResponseInvalidException(String message)
	{
		super(message);
	}
}
