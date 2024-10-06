package com.staples.payment.authorization.service;

import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.response.details.AuthRespCOFInfo;

public interface MitService
{
	AuthRespCOFInfo getMitInfo(AuthRequest authRequest);
}
