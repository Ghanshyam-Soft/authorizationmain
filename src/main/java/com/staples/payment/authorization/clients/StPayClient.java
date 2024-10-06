package com.staples.payment.authorization.clients;

import com.staples.payment.authorization.dto.staplespay.StPayAuthToken;
import com.staples.payment.authorization.dto.staplespay.StPayThreeDSRequest;
import com.staples.payment.authorization.dto.staplespay.StPayThreeDSResponse;
import com.staples.payment.shared.aci.response.ResponseWrapper;

public interface StPayClient
{
	ResponseWrapper<StPayAuthToken> getAuthToken(StPayThreeDSRequest tokenRequest);

	ResponseWrapper<StPayThreeDSResponse> get3DSResponse(StPayThreeDSRequest threeDSRequest, String authToken);
}
