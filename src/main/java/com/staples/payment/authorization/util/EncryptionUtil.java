package com.staples.payment.authorization.util;

import org.springframework.stereotype.Service;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.CreateTokenRequest;
import com.staples.payment.authorization.request.details.AuthReqTransactionHeader;

@Service
public class EncryptionUtil
{
	public String encryptMessageForLog(AuthRequest request)
	{
		final AuthReqTransactionHeader transactionHeader = request.getTransactionHeader();

		String temporarySolution = "";

		temporarySolution = temporarySolution + "ChildGUID " + transactionHeader.getChildGUID() + ", ";
		temporarySolution = temporarySolution + "ParentGUID " + transactionHeader.getParentGUID() + ", ";
		temporarySolution = temporarySolution + "OriginatingGUID " + transactionHeader.getOriginatingGUID() + ", ";
		temporarySolution = temporarySolution + "ReversalGUID " + transactionHeader.getReversalGUID() + ", ";
		temporarySolution = temporarySolution + "PaymentType " + transactionHeader.getPaymentType() + ", ";
		temporarySolution = temporarySolution + "OrderNumber " + transactionHeader.getOrderNumber() + ", ";
		temporarySolution = temporarySolution + "RequestType " + transactionHeader.getRequestType() + ", ";

		return temporarySolution; // TODO: print request in encrypted form
	}

	public String encryptMessageForLog(CreateTokenRequest request)
	{
		return request.toString(); // TODO: print request in encrypted form
	}
}