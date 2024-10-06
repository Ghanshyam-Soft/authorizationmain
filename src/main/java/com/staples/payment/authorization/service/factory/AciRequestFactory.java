package com.staples.payment.authorization.service.factory;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.shared.aci.request.AuthorizationCompleteRequest;
import com.staples.payment.shared.aci.request.AuthorizationRequest;
import com.staples.payment.shared.aci.request.BalanceRequest;
import com.staples.payment.shared.aci.request.CardVerificationRequest;
import com.staples.payment.shared.aci.request.PartialReversalRequest;
import com.staples.payment.shared.aci.request.RefundRequest;
import com.staples.payment.shared.aci.request.ReversalRequest;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.MerchantMaster;

public interface AciRequestFactory
{
	BalanceRequest createBalanceInquiry(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog);

	CardVerificationRequest createCardVerification(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog);

	AuthorizationRequest createAuthorization(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog);

	RefundRequest createRefund(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog);

	ReversalRequest createReversal(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog, AuthLog authLogToReverse);

	PartialReversalRequest createPartialReversal(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog, AuthLog authLogToReverse);

	AuthorizationCompleteRequest createAuthorizationComplete(AuthRequest gpasRequest, MerchantMaster merchantMaster, AuthLog authLog, AuthLog authLogToComplete);
}