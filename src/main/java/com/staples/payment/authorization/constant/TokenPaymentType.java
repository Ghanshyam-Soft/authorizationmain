package com.staples.payment.authorization.constant;

import com.staples.payment.shared.constant.PaymentMethod;

public enum TokenPaymentType
{
	PayPal(PaymentMethod.PP);

	private final PaymentMethod paymentMethod; // TokenPaymentType should likely not have been a separate enum but can't change it now

	private TokenPaymentType(PaymentMethod paymentMethod)
	{
		this.paymentMethod = paymentMethod;
	}

	public PaymentMethod getPaymentMethod()
	{
		return paymentMethod;
	}
}
