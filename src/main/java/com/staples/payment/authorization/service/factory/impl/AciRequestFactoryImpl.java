package com.staples.payment.authorization.service.factory.impl;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.staples.payment.authorization.configuration.properties.EnabledFeatures;
import com.staples.payment.authorization.constant.ECI;
import com.staples.payment.authorization.exception.InvalidInputException;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.request.details.AuthReqAddressInfo;
import com.staples.payment.authorization.request.details.AuthReqCOFInfo;
import com.staples.payment.authorization.request.details.AuthReqCardInfo;
import com.staples.payment.authorization.response.details.AuthRespCOFInfo;
import com.staples.payment.authorization.service.MitService;
import com.staples.payment.authorization.service.factory.AciRequestFactory;
import com.staples.payment.shared.aci.constants.AccountType;
import com.staples.payment.shared.aci.constants.PseudoBoolean;
import com.staples.payment.shared.aci.constants.ThreeDsAuthenticationMethod;
import com.staples.payment.shared.aci.constants.ThreeDsVersion;
import com.staples.payment.shared.aci.constants.TransactionCategory;
import com.staples.payment.shared.aci.constants.pos.CardPresent;
import com.staples.payment.shared.aci.constants.pos.EntryMode;
import com.staples.payment.shared.aci.constants.standingInstruction.TransactionMode;
import com.staples.payment.shared.aci.constants.standingInstruction.TransactionSource;
import com.staples.payment.shared.aci.constants.standingInstruction.TransactionType;
import com.staples.payment.shared.aci.request.AuthorizationCompleteRequest;
import com.staples.payment.shared.aci.request.AuthorizationRequest;
import com.staples.payment.shared.aci.request.AuthorizationRequest.AuthorizationRequestBuilder;
import com.staples.payment.shared.aci.request.BalanceRequest;
import com.staples.payment.shared.aci.request.CardVerificationRequest;
import com.staples.payment.shared.aci.request.PartialReversalRequest;
import com.staples.payment.shared.aci.request.RefundRequest;
import com.staples.payment.shared.aci.request.ReversalRequest;
import com.staples.payment.shared.aci.request.detail.Billing;
import com.staples.payment.shared.aci.request.detail.Card;
import com.staples.payment.shared.aci.request.detail.Customer;
import com.staples.payment.shared.aci.request.detail.DebitRequestCard;
import com.staples.payment.shared.aci.request.detail.DebitRequestPos;
import com.staples.payment.shared.aci.request.detail.Merchant;
import com.staples.payment.shared.aci.request.detail.Pos;
import com.staples.payment.shared.aci.request.detail.RefundMerchant;
import com.staples.payment.shared.aci.request.detail.ReversalPos;
import com.staples.payment.shared.aci.request.detail.StandingInstruction;
import com.staples.payment.shared.aci.request.detail.ThreeDSecure;
import com.staples.payment.shared.aci.request.detail.ThreeDSecure.ThreeDSecureBuilder;
import com.staples.payment.shared.constant.POSDataConstants.CardCaptureSource;
import com.staples.payment.shared.constant.PaymentMethod;
import com.staples.payment.shared.constant.PaymentType;
import com.staples.payment.shared.constant.cof.BillingType;
import com.staples.payment.shared.constant.cof.COFSchedInd;
import com.staples.payment.shared.constant.cof.StoredCredInd;
import com.staples.payment.shared.constant.cof.TransactionInitiator;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.BusinessMaster;
import com.staples.payment.shared.entity.MerchantMaster;
import com.staples.payment.shared.entity.ThreeDSResponse;
import com.staples.payment.shared.entity.aci.AciAuthResponse;

import lombok.val;

@Service
public class AciRequestFactoryImpl implements AciRequestFactory
{
	private final MitService mitService;
	private final EnabledFeatures enabledFeatures;
	private final YearMonth gcExpDate;
	private final ZoneId timezone;

	public AciRequestFactoryImpl(MitService mitService, EnabledFeatures enabledFeatures, @Value("${gc.exp.date}") YearMonth gcExpDate)
	{
		super();
		this.mitService = mitService;
		this.enabledFeatures = enabledFeatures;
		this.gcExpDate = gcExpDate;

		this.timezone = ZoneId.of("America/New_York");
	}

	@Override
	public BalanceRequest createBalanceInquiry(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog)
	{
		val transactionHeader = gpasRequest.getTransactionHeader();
		val transactionDetail = gpasRequest.getTransactionDetail();
		val cardInfo = gpasRequest.getCardInfo();
		val posDataCode = transactionDetail.getPosDataCode();

		Pos pos = createPos(merchantMaster, null);
		Card card = createCard(transactionHeader.getPaymentType(), transactionDetail.getGiftCardNumber(), cardInfo);

		val transactionCategory = determineTransactionCategory(posDataCode.getCardCaptureSource());

		BalanceRequest request = BalanceRequest.builder()
				.pos(pos)
				.card(card)
				.merchantTransactionId(authLog.getGpasKey())
				.tokenId(transactionDetail.getPaymentToken())
				.transactionCategory(transactionCategory)
				.mcc(merchantMaster.getMerchantCategoryCode())
				.build();

		return request;
	}

	@Override
	public CardVerificationRequest createCardVerification(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog) // TODO: Handle problems with empty Strings, either need to validate the requests for not having an empty string or need to clean the values
	{
		val transactionHeader = gpasRequest.getTransactionHeader();
		val transactionDetail = gpasRequest.getTransactionDetail();
		val cardInfo = gpasRequest.getCardInfo();
		val addressInfo = gpasRequest.getAddressInfo();
		val posDataCode = transactionDetail.getPosDataCode();

		val transactionCategory = determineTransactionCategory(posDataCode.getCardCaptureSource());
		val standingInstruction = createStandingInstruction(gpasRequest.getCofInfo());

		Pos pos = createPos(merchantMaster, standingInstruction);
		Card card = createCard(transactionHeader.getPaymentType(), transactionDetail.getGiftCardNumber(), cardInfo);
		Merchant merchant = addMerchantDetails(merchantMaster);
		Billing billing = createBilling(addressInfo);
		// Shipping shipping = createShipping(addressInfo);
		Customer customer = createCustomer(addressInfo);

		CardVerificationRequest request = CardVerificationRequest.builder()
				.pos(pos)
				.card(card)
				.merchant(merchant)
				.billing(billing)
				// .shipping(shipping)
				.customer(customer)
				.merchantTransactionId(authLog.getGpasKey())
				.tokenId(transactionDetail.getPaymentToken())
				.transactionCategory(transactionCategory)
				.standingInstruction(standingInstruction)
				.merchantUrl(transactionCategory == TransactionCategory.EC ? merchantMaster.getBusinessMaster().getMerchantUrl() : null)
				.build();

		return request;
	}

	@Override
	public AuthorizationRequest createAuthorization(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog)
	{
		val transactionHeader = gpasRequest.getTransactionHeader();
		val transactionDetail = gpasRequest.getTransactionDetail();
		val threeDSResponse = authLog.getThreeDSResponse();
		val cardInfo = gpasRequest.getCardInfo();
		val addressInfo = gpasRequest.getAddressInfo();
		val posDataCode = transactionDetail.getPosDataCode();

		val transactionCategory = determineTransactionCategory(posDataCode.getCardCaptureSource());
		val standingInstruction = createStandingInstruction(gpasRequest.getCofInfo());

		Pos pos = createPos(merchantMaster, standingInstruction);
		Card card = createCard(transactionHeader.getPaymentType(), transactionDetail.getGiftCardNumber(), cardInfo);
		Merchant merchant = addMerchantDetails(merchantMaster);
		Billing billing = createBilling(addressInfo);
		// Shipping shipping = createShipping(addressInfo);
		Customer customer = createCustomer(addressInfo);
		ThreeDSecure threeDSecure = createThreeDSecure(threeDSResponse);

		var requestBuilder = AuthorizationRequest.builder()
				.pos(pos)
				.card(card)
				.merchant(merchant)
				.billing(billing)
				.threeDSecure(threeDSecure)
				// .shipping(shipping)
				.customer(customer)
				.amount(transactionDetail.getTransactionAmount())
				.currency(transactionHeader.getCurrency().toString())
				.merchantTransactionId(authLog.getGpasKey())
				.merchantOrderNumber(transactionDetail.getPoNumber()) // CITI needs the PO number, hence if it is there it will be sent
				.tokenId(transactionDetail.getPaymentToken())
				.transactionCategory(transactionCategory)
				.partialApprovalAllowed(transactionHeader.getPaymentType() == PaymentType.Prepaid ? PseudoBoolean.True : null)
				.standingInstruction(standingInstruction)
				.merchantUrl(transactionCategory == TransactionCategory.EC ? merchantMaster.getBusinessMaster().getMerchantUrl() : null);

		requestBuilder = setMitValues(requestBuilder, gpasRequest, authLog, standingInstruction);

		return requestBuilder.build();
	}

	private AuthorizationRequestBuilder setMitValues(AuthorizationRequestBuilder requestBuilder, AuthRequest gpasRequest, AuthLog authLog, @Nullable StandingInstruction standingInstruction)
	{
		val paymentMethod = authLog.getPaymentMethod();

		if(standingInstruction != null && standingInstruction.getSource() == TransactionSource.MIT && standingInstruction.getMode() == TransactionMode.REPEATED
				&& List.of(PaymentMethod.VI, PaymentMethod.DI, PaymentMethod.MC, PaymentMethod.AM).contains(paymentMethod))
		{
			AuthRespCOFInfo mitInfo = mitService.getMitInfo(gpasRequest);

			if(paymentMethod == PaymentMethod.VI || paymentMethod == PaymentMethod.DI || paymentMethod == PaymentMethod.AM)
			{
				requestBuilder
						.bankTransactionId(mitInfo.getInitialTransactionId());
			}
			else if(paymentMethod == PaymentMethod.MC)
			{
				requestBuilder
						.banknetDate(mitInfo.getInitialTransactionDate())
						.banknetRefNr(mitInfo.getInitialTransactionId());
			}
		}

		return requestBuilder;
	}

	private @Nullable ThreeDSecure createThreeDSecure(@Nullable ThreeDSResponse threeDSResponse)
	{
		if(threeDSResponse != null && threeDSResponse.getTransStatus() != null && threeDSResponse.getTransStatus().equals("Y"))
		{
			ThreeDSecureBuilder threeDSecureBuilder = ThreeDSecure.builder();
			if(threeDSResponse.getEci() != null && !threeDSResponse.getEci().equals(ECI.ZEROSEVEN))
			{
				threeDSecureBuilder.dsTransactionId(threeDSResponse.getDsTransID())
						.authenticationMethod(ThreeDsAuthenticationMethod.ALL)
						.version(ThreeDsVersion.TWO)
						.verificationId(threeDSResponse.getEncodedCReq());
			}
			threeDSecureBuilder.eci(threeDSResponse.getEci());

			return threeDSecureBuilder.build();
		}
		return null;
	}

	@Override
	public AuthorizationCompleteRequest createAuthorizationComplete(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog, AuthLog authLogToComplete)
	{
		val transactionDetail = gpasRequest.getTransactionDetail();
		val cardInfo = gpasRequest.getCardInfo();
		val addressInfo = gpasRequest.getAddressInfo();
		val posDataCode = transactionDetail.getPosDataCode();

		val transactionCategory = determineTransactionCategory(posDataCode.getCardCaptureSource());

		DebitRequestPos pos = createDebitPos(merchantMaster);
		DebitRequestCard card = createDebitRequestCard(transactionDetail.getGiftCardNumber(), cardInfo);
		Merchant merchant = addMerchantDetails(merchantMaster);
		Billing billing = createBilling(addressInfo);
		Customer customer = createCustomer(addressInfo);

		var requestBuilder = AuthorizationCompleteRequest.builder()
				.merchantTransactionId(authLog.getGpasKey())
				.merchantOrderNumber(transactionDetail.getPoNumber())
				.transactionCategory(transactionCategory)
				.pos(pos)
				.card(card)
				.merchant(merchant)
				.billing(billing)
				.customer(customer)
				.amount(transactionDetail.getTransactionAmount());

		AciAuthResponse aciResponse = authLogToComplete.getAciResponse();
		if(aciResponse != null)
		{
			requestBuilder.referencedPaymentId(aciResponse.getAciTransactionId());
		}
		else
		{
			// requestBuilder.referencedMerchantTransactionId(authLogToReverse.getGpasKey());
			// TODO: May eventually want this to be a decline rather than an error - per Pratima
			throw new InvalidInputException("The AuthLog to be completed does not have an associated Response from ACI.");
		}

		if(transactionCategory == TransactionCategory.EC)
		{
			requestBuilder
					.merchantUrl(merchantMaster.getBusinessMaster().getMerchantUrl());
		}

		return requestBuilder.build();
	}

	@Override
	public RefundRequest createRefund(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog)
	{
		val transactionHeader = gpasRequest.getTransactionHeader();
		val transactionDetail = gpasRequest.getTransactionDetail();
		val cardInfo = gpasRequest.getCardInfo();
		val addressInfo = gpasRequest.getAddressInfo();
		val posDataCode = transactionDetail.getPosDataCode();

		val transactionCategory = determineTransactionCategory(posDataCode.getCardCaptureSource());
		StandingInstruction standingInstruction = null; // Per Pratima, COF isn't used for refunds

		Pos pos = createPos(merchantMaster, standingInstruction);
		Card card = createCard(transactionHeader.getPaymentType(), transactionDetail.getGiftCardNumber(), cardInfo);
		RefundMerchant refundMerchant = addRefundMerchantDetails(merchantMaster);
		Billing billing = createBilling(addressInfo);
		// Shipping shipping = createShipping(addressInfo);
		Customer customer = createCustomer(addressInfo);

		RefundRequest request = RefundRequest.builder()
				.pos(pos)
				.card(card)
				.merchant(refundMerchant)
				.billing(billing)
				// .shipping(shipping)
				.customer(customer)
				.amount(transactionDetail.getTransactionAmount())
				.currency(transactionHeader.getCurrency().toString())
				.merchantTransactionId(authLog.getGpasKey())
				.tokenId(transactionDetail.getPaymentToken())
				.transactionCategory(transactionCategory)
				.partialApprovalAllowed(transactionHeader.getPaymentType() == PaymentType.Prepaid ? PseudoBoolean.True : null)
				.standingInstruction(standingInstruction)
				.merchantUrl(transactionCategory == TransactionCategory.EC ? merchantMaster.getBusinessMaster().getMerchantUrl() : null)
				.build();

		return request;
	}

	@Override
	public ReversalRequest createReversal(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog, AuthLog authLogToReverse)
	{
		val transactionDetail = gpasRequest.getTransactionDetail();
		val addressInfo = gpasRequest.getAddressInfo();
		val posDataCode = transactionDetail.getPosDataCode();

		val transactionCategory = determineTransactionCategory(posDataCode.getCardCaptureSource());

		ReversalPos pos = createReversalPos(merchantMaster);
		Merchant merchant = addMerchantDetails(merchantMaster);
		Billing billing = createBilling(addressInfo);
		// Shipping shipping = createShipping(addressInfo);
		Customer customer = createCustomer(addressInfo);

		var requestBuilder = ReversalRequest.builder()
				.requestId(authLog.getGpasKey())
				.transactionCategory(transactionCategory)
				.pos(pos)
				.merchant(merchant)
				.billing(billing)
				// .shipping(shipping)
				.customer(customer);

		AciAuthResponse aciResponse = authLogToReverse.getAciResponse();
		if(aciResponse != null)
		{
			requestBuilder.referencedPaymentId(aciResponse.getAciTransactionId());
		}
		else
		{
			// requestBuilder.referencedMerchantTransactionId(authLogToReverse.getGpasKey());
			// TODO: May eventually want this to be a decline rather than an error - per Pratima
			throw new InvalidInputException("The AuthLog to be reversed does not have an associated Response from ACI.");
		}

		if(transactionCategory == TransactionCategory.EC)
		{
			requestBuilder
					.merchantUrl(merchantMaster.getBusinessMaster().getMerchantUrl());
		}

		return requestBuilder.build();
	}

	@Override
	public PartialReversalRequest createPartialReversal(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog, AuthLog authLogToReverse)
	{
		val transactionHeader = gpasRequest.getTransactionHeader();
		val transactionDetail = gpasRequest.getTransactionDetail();
		val addressInfo = gpasRequest.getAddressInfo();
		val posDataCode = transactionDetail.getPosDataCode();

		val transactionCategory = determineTransactionCategory(posDataCode.getCardCaptureSource());

		ReversalPos pos = createReversalPos(merchantMaster);
		Merchant merchant = addMerchantDetails(merchantMaster);
		Billing billing = createBilling(addressInfo);
		// Shipping shipping = createShipping(addressInfo);
		Customer customer = createCustomer(addressInfo);

		var requestBuilder = PartialReversalRequest.builder()
				.requestId(authLog.getGpasKey())
				.transactionCategory(transactionCategory)
				.amount(authLog.getTransactionAmount())// TODO: Is the right value? Do I need to worry about the customers sending a negative value/being the value to subtract off the last amount
				.currency(transactionHeader.getCurrency().toString())
				.pos(pos)
				.merchant(merchant)
				.billing(billing)
				// .shipping(shipping)
				.customer(customer);

		AciAuthResponse aciResponse = authLogToReverse.getAciResponse();
		if(aciResponse != null)
		{
			requestBuilder.referencedPaymentId(aciResponse.getAciTransactionId());
		}
		else
		{
			// requestBuilder.referencedMerchantTransactionId(authLogToReverse.getGpasKey());
			// TODO: May eventually want this to be a decline rather than an error - per Pratima
			throw new InvalidInputException("The AuthLog to be reversed does not have an associated Response from ACI.");
		}

		if(transactionCategory == TransactionCategory.EC)
		{
			requestBuilder
					.merchantUrl(merchantMaster.getBusinessMaster().getMerchantUrl());
		}

		return requestBuilder.build();
	}

	private TransactionCategory determineTransactionCategory(CardCaptureSource cardCaptureSource) // TODO: Do we want to move this logic into one of the two enums
	{
		if(cardCaptureSource == CardCaptureSource.INT)
		{
			return TransactionCategory.EC; // eCommerce
		}
		else if(cardCaptureSource == CardCaptureSource.PHN)
		{
			return TransactionCategory.TO; // Telephone order
		}
		else if(cardCaptureSource == CardCaptureSource.MAL || cardCaptureSource == CardCaptureSource.FAX)
		{
			return TransactionCategory.MO; // Mail order
		}
		else if(cardCaptureSource == CardCaptureSource.TER)
		{
			return TransactionCategory.PO; // POS
		}
		else
		{
			throw new RuntimeException("Invalid CardCaptureSource type");
		}
	}

	private DebitRequestPos createDebitPos(MerchantMaster merchantMaster)
	{
		return DebitRequestPos.builder()
				.terminalId(merchantMaster.getBankTerminalId())
				.storeId(merchantMaster.getBusinessMaster().getGpasStoreNo().toString())// merchantMaster.getBankMerchantId()) //we won't be sending actual MID to ACI now onwards, ACI will decide what to send depending on storeId value
				.entryMode(EntryMode.UNKNOWN)
				.cardPresent(CardPresent.CARD_NOT_PRESENT)
				.build();
	}

	private ReversalPos createReversalPos(MerchantMaster merchantMaster)
	{
		ReversalPos pos = ReversalPos.builder()
				.terminalId(merchantMaster.getBankTerminalId())
				.build();

		return pos;
	}

	private Merchant addMerchantDetails(MerchantMaster merchantMaster)
	{
		BusinessMaster businessMaster = merchantMaster.getBusinessMaster();

		return Merchant.builder()
				.mcc(merchantMaster.getMerchantCategoryCode())
				.street(businessMaster.getMerchantAddress())
				.city(businessMaster.getMerchantCity())
				.state(businessMaster.getMerchantState())
				.country(businessMaster.getCountryCode().substring(0, 2))
				.name(businessMaster.getMerchantName())
				.phone(businessMaster.getMerchantCustServiceNo())
				.postcode(businessMaster.getMerchantZipCode())
				.taxId(businessMaster.getMerchantTaxId())
				.submerchantId(null)
				.receivingInstitutionCode(null)
				.build();
	}

	private RefundMerchant addRefundMerchantDetails(MerchantMaster merchantMaster)
	{
		BusinessMaster businessMaster = merchantMaster.getBusinessMaster();

		return RefundMerchant.builder()
				.mcc(merchantMaster.getMerchantCategoryCode())
				.street(businessMaster.getMerchantAddress())
				.city(businessMaster.getMerchantCity())
				.state(businessMaster.getMerchantState())
				.country(businessMaster.getCountryCode().substring(0, 2))
				.name(businessMaster.getMerchantName())
				.phone(businessMaster.getMerchantCustServiceNo())
				.postcode(businessMaster.getMerchantZipCode())
				.taxId(businessMaster.getMerchantTaxId())
				.receivingInstitutionCode(null)
				.build();
	}

	private Card createCard(PaymentType paymentType, String giftCardNumber, final AuthReqCardInfo cardInfo)
	{
		final AccountType accountType;
		String cardNumber = null;
		YearMonth expiryDate = cardInfo.getExpirationDate();

		if(paymentType == PaymentType.GiftCard)
		{
			accountType = AccountType.STORED_VALUE;
			cardNumber = giftCardNumber;
			expiryDate = manipulateExpiryDate(expiryDate);
		}

		else
		{
			accountType = AccountType.CREDIT;
		}

		Card card = Card.builder()
				.number(cardNumber)
				.expirationDate(expiryDate)
				.accountType(accountType)
				.cvv(cardInfo.getCcin())
				.build();

		return card;
	}

	private DebitRequestCard createDebitRequestCard(String giftCardNumber, final AuthReqCardInfo cardInfo)
	{
		AccountType accountType = AccountType.STORED_VALUE;
		String cardNumber = giftCardNumber;
		YearMonth expiryDate = manipulateExpiryDate(cardInfo.getExpirationDate());

		DebitRequestCard debitRequestCard = DebitRequestCard.builder()
				.number(cardNumber)
				.expirationDate(expiryDate)
				.accountType(accountType)
				.cvv(cardInfo.getCcin())
				.build();

		return debitRequestCard;
	}

	private YearMonth manipulateExpiryDate(YearMonth expirationDate)
	{
		final YearMonth currentYearMonth = YearMonth.now(timezone);
		final YearMonth newExpiryDate;

		if(null != expirationDate && expirationDate.isAfter(currentYearMonth))
		{
			newExpiryDate = expirationDate;
		}
		else
		{
			newExpiryDate = gcExpDate;
		}

		return newExpiryDate;
	}

	private Pos createPos(MerchantMaster merchantMaster, @Nullable StandingInstruction standingInstruction)
	{
		val entryMode = determineEntryMode(standingInstruction);
		// TerminalType terminalType = TerminalType.ECOM_SECURE_NO_CRDHLDR_AUTH;

		Pos pos = Pos.builder()
				.terminalId(merchantMaster.getBankTerminalId())
				.storeId(merchantMaster.getBusinessMaster().getGpasStoreNo().toString()) // we won't be sending actual MID to ACI now onwards, ACI will decide what to send depending on storeId value
				.entryMode(entryMode)
				.pinCaptureCapability(null)
				// .terminalType(terminalType) //It's not mandatory as per Azhar, hence ignoring this for now.
				.build();

		return pos;
	}

	private EntryMode determineEntryMode(@Nullable StandingInstruction standingInstruction)
	{
		final boolean isMit = (standingInstruction != null && standingInstruction.getSource() == TransactionSource.MIT);
		final boolean isCitRepeated = (standingInstruction != null && standingInstruction.getSource() == TransactionSource.CIT && standingInstruction.getMode() == TransactionMode.REPEATED);

		if(isMit || isCitRepeated)

		{
			return EntryMode.CREDENTIAL_ON_FILE;
		}
		else
		{
			return EntryMode.UNKNOWN;
		}
	}

	private @Nullable Billing createBilling(@Nullable final AuthReqAddressInfo addressInfo)
	{
		if(addressInfo == null)
		{
			return null;
		}

		Billing billing = Billing.builder()
				.street1(addressInfo.getBillToAddress1())
				.street2(addressInfo.getBillToAddress2())
				.city(addressInfo.getBillToCity())
				.state(addressInfo.getBillToState())
				.postcode(addressInfo.getBillToZipCode())
				.country(addressInfo.getBillToCountry())
				.build();

		return billing;
	}

	private Customer createCustomer(@Nullable final AuthReqAddressInfo addressInfo)
	{
		var customerBuilder = Customer.builder();

		/*		customerBuilder
						.orderNumber(null)
						.companyName(null)
						.taxExempt(null)
						.orderDate(null)
						.customerCode(null)
						.merchantReferenceNumber(null);*/

		if(addressInfo != null)
		{
			customerBuilder
					// .givenName(addressInfo.getBillToFirstName()) //Excluding these two because interfering with AMEX auth (and removing them shouldn't hurt Fiserv auth)
					// .surname(addressInfo.getBillToLastName())
					.email(addressInfo.getBillToEmail());
		}

		return customerBuilder.build();
	}

	/*
	private @Nullable Shipping createShipping(@Nullable final AuthReqAddressInfo addressInfo) //Excluding this this because interfering with AMEX auth (and removing it shouldn't hurt Fiserv auth)
	{
		if(addressInfo == null)
		{
			return null;
		}
	
		Shipping shipping = Shipping.builder()
				.street1(addressInfo.getShipToAddress1())
				.street2(addressInfo.getShipToAddress2())
				.city(addressInfo.getShipToCity())
				.state(addressInfo.getShipToState())
				.postcode(addressInfo.getShipToZipCode())
				.country(addressInfo.getShipToCountry())
				.build();
	
		return shipping;
	}
	// */

	private @Nullable StandingInstruction createStandingInstruction(@Nullable AuthReqCOFInfo cofInfo)
	{
		if(!enabledFeatures.isCit() && !enabledFeatures.isMit())
		{
			return null;
		}

		if(cofInfo == null)
		{
			return null;
		}

		// TODO: Should we validate it so MIT will always have a transaction type and such dependencies?

		val transactionSource = determineTransactionSource(cofInfo.getTransactionInitiator());
		if((!enabledFeatures.isCit() && transactionSource == TransactionSource.CIT)
				|| (!enabledFeatures.isMit() && transactionSource == TransactionSource.MIT))
		{
			return null;
		}

		val transactionType = determineTransactionType(cofInfo.getCofSchedInd(), cofInfo.getBillingType());
		val transactionMode = determineTransactionMode(cofInfo.getStoredCredInd());

		if(transactionSource != null && transactionMode == null) // TODO: Should this validation be happening earlier.
		{
			throw new InvalidInputException("If TransactionInitiator is present, StoredCredInd must not be null");
		}

		return StandingInstruction.builder()
				.source(transactionSource)
				.mode(transactionMode)
				.type(transactionType)
				// .industryPractice(null) //From the documentation "this field cannot be specified in addition to standingInstruction.type."
				.build();
	}

	private @Nullable TransactionSource determineTransactionSource(@Nullable TransactionInitiator transactionInitiator)
	{
		if(transactionInitiator == null)
		{
			return null;
		}
		else if(transactionInitiator == TransactionInitiator.Merchant)
		{
			return TransactionSource.MIT;
		}
		else if(transactionInitiator == TransactionInitiator.Customer)
		{
			return TransactionSource.CIT;
		}
		else
		{
			throw new RuntimeException("Invalid TransactionInitiator type");
		}
	}

	private @Nullable TransactionType determineTransactionType(@Nullable COFSchedInd schedInd, @Nullable BillingType billingType)
	{
		if(schedInd == null)
		{
			return null;
		}
		else if(schedInd == COFSchedInd.Scheduled)
		{
			if(billingType == null)// TODO: If null, should we thrown an error (or possibly do we need to validate ahead of time)? Since if the transaction is MIT, it needs this value
			{
				return null;
			}
			else if(billingType == BillingType.RECURRING)
			{
				return TransactionType.RECURRING;
			}
			else if(billingType == BillingType.INSTALLMENT)
			{
				return TransactionType.INSTALLMENT;
			}
			else
			{
				throw new RuntimeException("Invalid BillingType type");
			}
		}
		else if(schedInd == COFSchedInd.UnScheduled)
		{
			return TransactionType.UNSCHEDULED;
		}
		else
		{
			throw new RuntimeException("Invalid COFSchedIndEnum type");
		}
	}

	private @Nullable TransactionMode determineTransactionMode(@Nullable StoredCredInd storedCredInd)
	{
		if(storedCredInd == null)
		{
			return null;
		}
		else if(storedCredInd == StoredCredInd.Initial)
		{
			return TransactionMode.INITIAL;
		}
		else if(storedCredInd == StoredCredInd.Subsequent)
		{
			return TransactionMode.REPEATED;
		}
		else
		{
			throw new RuntimeException("Invalid StoredCredIndEnum type");
		}
	}
}