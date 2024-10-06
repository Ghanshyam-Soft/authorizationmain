package com.staples.payment.authorization.service;

import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.exceptions.CustomException;

public interface VendorRouter
{
	AuthLog routePayment(AuthRequest request, AuthLog authLog) throws CustomException, InvalidInputException;
}