package com.staples.payment.authorization.service.impl;

import org.springframework.stereotype.Service;

import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.service.VendorRouter;
import com.staples.payment.authorization.service.bank.AciPaymentService;
import com.staples.payment.authorization.service.bank.AmexService;
import com.staples.payment.authorization.service.bank.BamboraPaymentService;
import com.staples.payment.authorization.service.bank.BraintreePaymentService;
import com.staples.payment.authorization.service.bank.CybersourcePaymentService;
import com.staples.payment.shared.constant.Bank;
import com.staples.payment.shared.constant.PaymentMethod;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.MerchantMaster;
import com.staples.payment.shared.exceptions.CustomException;

@Service
public class VendorRouterImpl implements VendorRouter
{
	private final AciPaymentService aciPaymentService;
	private final BraintreePaymentService braintreePaymentService;
	private final BamboraPaymentService bamboraPaymentService;
	private final AmexService amexService;
	private final CybersourcePaymentService cybersourcePaymentService;

	public VendorRouterImpl(AciPaymentService aciPaymentService, BraintreePaymentService braintreePaymentService, BamboraPaymentService bamboraPaymentService, AmexService amexService,
			CybersourcePaymentService cybersourcePaymentService)
	{
		this.aciPaymentService = aciPaymentService;
		this.braintreePaymentService = braintreePaymentService;
		this.bamboraPaymentService = bamboraPaymentService;
		this.amexService = amexService;
		this.cybersourcePaymentService = cybersourcePaymentService;
	}

	@Override
	public AuthLog routePayment(AuthRequest request, AuthLog authLog) throws CustomException, InvalidInputException
	{
		final Bank bank = authLog.getBank();
		final MerchantMaster merchantMaster = authLog.getMerchantMaster();

		AuthLog returnedAuthLog;
		if(bank == Bank.ACI)
		{
			returnedAuthLog = aciPaymentService.process(authLog, request, merchantMaster);

			if(returnedAuthLog.getPaymentMethod() == PaymentMethod.AM)
			{
				returnedAuthLog = amexService.processPayPoints(returnedAuthLog, request);
			}
		}
		else if(bank == Bank.BRAINTREE)
		{
			returnedAuthLog = braintreePaymentService.process(authLog, request);
		}
		else if(bank == Bank.BAMBORA)
		{
			returnedAuthLog = bamboraPaymentService.process(authLog, request, merchantMaster);
		}
		else if(bank == Bank.CYB)
		{
			returnedAuthLog = cybersourcePaymentService.process(authLog, request, merchantMaster);
		}
		else
		{
			throw new RuntimeException("Invalid bankId");
		}

		return returnedAuthLog;
	}
}