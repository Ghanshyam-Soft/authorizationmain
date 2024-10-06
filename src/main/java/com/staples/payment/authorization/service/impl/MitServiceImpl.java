package com.staples.payment.authorization.service.impl;

import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.staples.payment.authorization.clients.CardConsentClient;
import com.staples.payment.authorization.constant.CardConsentStatus;
import com.staples.payment.authorization.dto.cardConsent.CardConsentKey;
import com.staples.payment.authorization.dto.cardConsent.CardConsentResponse;
import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.response.details.AuthRespCOFInfo;
import com.staples.payment.authorization.service.MitService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MitServiceImpl implements MitService
{
	private final CardConsentClient cardConsentClient;

	private final DateTimeFormatter expiryDatePattern = DateTimeFormatter.ofPattern("MM/yy");

	public MitServiceImpl(CardConsentClient cardConsentClient)
	{
		this.cardConsentClient = cardConsentClient;
	}

	@Override
	public AuthRespCOFInfo getMitInfo(AuthRequest request)
	{
		String consentClientId = request.getTransactionDetail().getConsentClientId();
		String customerId = request.getTransactionDetail().getCustomerId();
		String businessUnit = request.getTransactionHeader().getBusinessUnit();
		String division = request.getTransactionHeader().getDivision();

		List<CardConsentResponse> storedCredentials = cardConsentClient.getStoredCredentials(consentClientId, customerId, businessUnit, division);

		final String paymentToken = request.getTransactionDetail().getPaymentToken();
		final CardConsentResponse storedCredential = getStoredCredential(storedCredentials, paymentToken);

		return AuthRespCOFInfo.builder()
				.initialTransactionDate(storedCredential.getInitialTransactionDate())
				.initialTransactionId(storedCredential.getInitialTransactionId())
				.build();
	}

	private @Nullable CardConsentResponse getStoredCredential(@Nullable List<CardConsentResponse> storedCredentials, final String paymentToken)
	{
		if(storedCredentials == null)
		{
			throw new InvalidInputException("No stored credentials returned from consent service for this input.");
		}

		for(CardConsentResponse storedCredential : storedCredentials)
		{
			if(storedCredential != null)
			{
				final CardConsentKey cardConsentKey = storedCredential.getCardConsentKey();
				final Instant optOutDatetime = storedCredential.getOptOutDatetime();
				final YearMonth expiryDate = convertToYearMonth(storedCredential.getCardExpirationDate());

				boolean matchingAliasNumber = cardConsentKey != null && paymentToken.equals(cardConsentKey.getAn());
				boolean isActive = CardConsentStatus.Active.equals(storedCredential.getStatus());
				boolean notOptedOut = (optOutDatetime != null) ? optOutDatetime.isAfter(Instant.now()) : true;
				boolean hasFutureExpiryDate = (expiryDate != null) ? !expiryDate.isBefore(YearMonth.now()) : false;

				if(matchingAliasNumber && isActive && notOptedOut && hasFutureExpiryDate)
				{
					log.info("storedCredential {}", storedCredential);
					return storedCredential;
				}
			}
		}

		throw new InvalidInputException("No matching stored credential.");
	}

	private @Nullable YearMonth convertToYearMonth(final @Nullable String expiryDateString)
	{
		try
		{
			// TODO: VTA pass CCS MM/yy. However there is no restriction on CCS end for this so that restriction needs to be added.

			return (expiryDateString != null) ? YearMonth.parse(expiryDateString, expiryDatePattern) : null;
		}
		catch(Exception e)
		{
			log.warn("Failed to convert CardExpirationDate to YearMonth format", e);
			return null;
		}
	}
}
