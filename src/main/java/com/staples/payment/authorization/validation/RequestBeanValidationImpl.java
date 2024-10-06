package com.staples.payment.authorization.validation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.details.AuthReqTransactionDetail;
import com.staples.payment.authorization.request.details.AuthReqTransactionHeader;
import com.staples.payment.shared.constant.AuthRequestType;
import com.staples.payment.shared.constant.PaymentType;
import com.staples.payment.shared.constant.TokenType;
import com.staples.payment.shared.util.CardNumberUtil;

import lombok.val;

@Component
public class RequestBeanValidationImpl implements RequestBeanValidation
{
	private final CardNumberUtil cardNoUtil;

	public RequestBeanValidationImpl(CardNumberUtil cardNoUtil)
	{
		super();
		this.cardNoUtil = cardNoUtil;
	}

	@Override
	public void validateSpecificFields(final AuthRequest request) throws InvalidInputException
	{
		final AuthReqTransactionDetail transactionDetail = request.getTransactionDetail();
		final AuthReqTransactionHeader transactionHeader = request.getTransactionHeader();

		final PaymentType paymentType = transactionHeader.getPaymentType();
		final AuthRequestType requestType = transactionHeader.getRequestType();

		validateRequestPaymentCombination(transactionHeader);
		validateFieldsByRequestType(transactionHeader);

		if(paymentType != PaymentType.GiftCard && requestType != AuthRequestType.Lookup)
		{
			validateAccountInformation(transactionDetail, transactionHeader.getPaymentType());
		}
		else if(paymentType == PaymentType.GiftCard)
		{
			validateGiftCardNumber(transactionDetail.getGiftCardNumber());
		}
	}

	private void validateFieldsByRequestType(AuthReqTransactionHeader transactionHeader) throws InvalidInputException
	{
		val requestType = transactionHeader.getRequestType();

		// aka for all non preauth, order number is required
		if(requestType != AuthRequestType.PreAuthorization)
		{
			final String orderNumber = transactionHeader.getOrderNumber();
			if(orderNumber == null || orderNumber.isBlank())
			{
				throw new InvalidInputException("OrderNumber can't be null or blank for RequestType " + requestType);
			}
		}

		if(requestType == AuthRequestType.Reversal || requestType == AuthRequestType.PartialReversal)
		{
			final String reversalGUID = transactionHeader.getReversalGUID();
			if(reversalGUID == null || reversalGUID.isBlank())
			{
				throw new InvalidInputException("ReversalGUID can't be null or blank for RequestType " + requestType);
			}
		}
		else if(requestType == AuthRequestType.Lookup)
		{
			final String originatingGUID = transactionHeader.getOriginatingGUID();
			if(originatingGUID == null || originatingGUID.isBlank())
			{
				throw new InvalidInputException("OriginatingGUID can't be null or blank for RequestType " + requestType);
			}
		}
	}

	private void validateRequestPaymentCombination(final AuthReqTransactionHeader transactionHeader) throws InvalidInputException
	{
		final AuthRequestType requestType = transactionHeader.getRequestType();
		final PaymentType paymentType = transactionHeader.getPaymentType();

		if(!paymentType.getValidAuthRequestTypes().contains(requestType))
		{
			throw new InvalidInputException("RequestType " + requestType + " and PaymentType " + paymentType + " are not a valid combination.");
		}
	}

	private void validateAccountInformation(final AuthReqTransactionDetail transactionDetail, PaymentType paymentType) throws InvalidInputException
	{
		val paymentToken = transactionDetail.getPaymentToken();
		val temporaryToken = transactionDetail.getTemporaryPaymentToken();

		boolean paymentTokenPresent = StringUtils.hasText(paymentToken);
		boolean temporaryTokenPresent = StringUtils.hasText(temporaryToken);

		if(!paymentTokenPresent && !temporaryTokenPresent)
		{
			throw new InvalidInputException("PaymentToken and TemporaryPaymentToken are all empty or null!!! ");
		}

		if(paymentTokenPresent)
		{
			if(paymentType != PaymentType.PayPal)
			{
				validateAliasNumber(paymentToken);
			}
		}

		// Currently there isn't a validation we know we can do for temporaryTokenPresent
	}

	private void validateAliasNumber(String aliasAccount) throws InvalidInputException
	{
		if(cardNoUtil.isCardLengthValid(aliasAccount))
		{
			TokenType tokenType = cardNoUtil.getTokenFormat(aliasAccount);

			if(tokenType != TokenType.AN) // If we begin supporting LOS need to add it here.
			{
				throw new InvalidInputException("Alias Account is received in unexpected format, not AN !!!" + aliasAccount);
			}
		}
		else
		{
			throw new InvalidInputException("Alias Account is received with unexpected length !!!" + aliasAccount);
		}
	}

	private void validateGiftCardNumber(String giftCardNumber) throws InvalidInputException
	{
		boolean giftCardNumberPresent = StringUtils.hasText(giftCardNumber);

		if(!giftCardNumberPresent || !giftCardNumber.chars().allMatch(Character::isDigit))
		{
			throw new InvalidInputException("GiftCardNumber is received as null or non-numeric !!!");
		}
	}
}