package com.staples.payment.authorization.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staples.payment.authorization.clients.StPayClient;
import com.staples.payment.authorization.configuration.properties.EnabledFeatures;
import com.staples.payment.authorization.configuration.properties.StaplesPayConfig;
import com.staples.payment.authorization.constant.ThreeDsConstants;
import com.staples.payment.authorization.dto.staplespay.StPayAuthToken;
import com.staples.payment.authorization.dto.staplespay.StPayThreeDSRequest;
import com.staples.payment.authorization.dto.staplespay.StPayThreeDSResponse;
import com.staples.payment.authorization.dto.staplespay.StPayThreeDSResponseStatus;
import com.staples.payment.authorization.request.AuthRequest;
import com.staples.payment.authorization.service.ThreeDSService;
import com.staples.payment.shared.aci.response.ResponseWrapper;
import com.staples.payment.shared.entity.AuthLog;
import com.staples.payment.shared.entity.ThreeDSResponse;
import com.staples.payment.shared.repo.ThreeDSRepo;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ThreeDSServiceImpl implements ThreeDSService
{
	private static final Logger audit = LoggerFactory.getLogger("audit-log");

	private final StaplesPayConfig staplesPayConfig;
	private final StPayClient stPayClient;
	private final ObjectMapper objectMapper;
	private final ThreeDSRepo threeDSRepo;
	private final EnabledFeatures enabledFeatures;

	private StPayAuthToken stPayToken = null;

	public ThreeDSServiceImpl(StaplesPayConfig staplesPayConfig, ObjectMapper objectMapper, StPayClient stPayClient, ThreeDSRepo threeDSRepo, EnabledFeatures enabledFeatures)
	{
		this.staplesPayConfig = staplesPayConfig;
		this.stPayClient = stPayClient;
		this.objectMapper = objectMapper;
		this.threeDSRepo = threeDSRepo;
		this.enabledFeatures = enabledFeatures;
	}

	@Override
	public AuthLog set3dsProperties(final AuthLog authLog, final AuthRequest request)
	{
		if(enabledFeatures.isThreeDs())
		{
			if(authLog.isThreeDsFlag())
			{
				ThreeDSResponse threeDSResponse = fetch3dsAttributes(request, authLog);

				authLog.setThreeDSResponse(threeDSResponse);
				threeDSRepo.update(threeDSResponse);
			}
		}

		return authLog;
	}

	private ThreeDSResponse fetch3dsAttributes(AuthRequest authRequest, AuthLog authLog)
	{
		String threeDSServerTransID = authRequest.getCardInfo().getXid();

		StPayThreeDSRequest threeDSRequest = this.createStPayRequest(threeDSServerTransID);

		ThreeDSResponse preResponseData = insertIntoThreeDsResponse(authLog, threeDSServerTransID, threeDSRequest.getMerchantReferenceID());

		ThreeDSResponse response = this.get3DSResponse(threeDSRequest, preResponseData, authLog.getChildKey(), authLog.getGpasKey());

		return response;
	}

	private ThreeDSResponse insertIntoThreeDsResponse(AuthLog authLog, String threeDSServerTransID, String merchantRefId)
	{
		ThreeDSResponse threeDSResponse = ThreeDSResponse.builder()
				.gpasKey(authLog.getGpasKey())
				.threeDSServerTransID(threeDSServerTransID)
				.merchantReferenceId(merchantRefId)
				.build();

		authLog.setThreeDSResponse(threeDSResponse);
		threeDSRepo.insert(threeDSResponse);

		return threeDSResponse;
	}

	private ThreeDSResponse get3DSResponse(StPayThreeDSRequest threeDSRequest, ThreeDSResponse preThreeDsResonse, String childGuid, String gpasGuid)
	{
		final String authToken = generateAuthToken(childGuid);

		final Instant startTime = Instant.now();
		audit.info("started calling staplespay for 3DS against childGuid: {} and merchantTransId: {} ", childGuid, threeDSRequest.getMerchantID());

		ResponseWrapper<StPayThreeDSResponse> stPayThreeDSResponse = stPayClient.get3DSResponse(threeDSRequest, authToken);

		final long duration = Duration.between(startTime, Instant.now()).toMillis();
		audit.info("finished calling staplespay for 3DS against childGuid: {} and merchantTransId: {} and time taken {} ms", childGuid, threeDSRequest.getMerchantID(), duration);

		ThreeDSResponse threeDSResponse = createThreeDSResponse(stPayThreeDSResponse, preThreeDsResonse, threeDSRequest.getMerchantReferenceID(), gpasGuid);
		return threeDSResponse;
	}

	private StPayThreeDSRequest createStPayRequest(String threeDSServerTransID)
	{
		final String merchantReferenceID = UUID.randomUUID().toString();

		return StPayThreeDSRequest.builder()
				.merchantID(staplesPayConfig.getMerchantId())
				.merchantReferenceID(merchantReferenceID)
				.threeDSServerTransID(threeDSServerTransID)
				.build();
	}

	private String generateAuthToken(String childGuid)
	{
		if(stPayToken != null && stPayToken.getToken().getValidUntil() >= System.currentTimeMillis())
		{
			String existingAuthToken = stPayToken.getToken().getValue();
			log.info("Existing authToken used: {}", existingAuthToken);
			return existingAuthToken;
		}
		else
		{
			StPayThreeDSRequest tokenRequest = createStPayRequest(null);

			final Instant startTime = Instant.now();
			audit.info("started calling staplespay for authtoken against childGuid: {} and merchantTransId: {} ", childGuid, tokenRequest.getMerchantID());

			ResponseWrapper<StPayAuthToken> tokenResponse = stPayClient.getAuthToken(tokenRequest);

			final long duration = Duration.between(startTime, Instant.now()).toMillis();
			audit.info("finished calling staplespay for authtoken against childGuid: {} and merchantTransId: {} and time taken {} ms", childGuid, tokenRequest.getMerchantID(), duration);

			if(tokenResponse.getStatusCode().is2xxSuccessful()
					&& tokenResponse.getResponseBody().getToken() != null
					&& !tokenResponse.getResponseBody().getToken().getValue().isBlank())
			{
				stPayToken = tokenResponse.getResponseBody();
				String newAuthToken = stPayToken.getToken().getValue();
				log.info("Fresh authToken created: {}", newAuthToken);

				Long validUntil = stPayToken.getToken().getValidUntil();
				audit.info("Fresh authToken created and it is valid until: {}", validUntil); // This is for validation on production
				stPayToken.getToken().setValidUntil(validUntil + staplesPayConfig.getExpireIn());

				return newAuthToken;
			}
			else
			{
				// TODO: Is it okay to be printing the entire response?
				throw new RuntimeException("Issue with staplespay token generation for response " + tokenResponse);
			}
		}
	}

	private ThreeDSResponse createThreeDSResponse(ResponseWrapper<StPayThreeDSResponse> stPayResponseWrapper, ThreeDSResponse response, String merchantReferenceID, String gpasGuid)
	{
		final StPayThreeDSResponse threeDSResponseDto = stPayResponseWrapper.getResponseBody();
		if(threeDSResponseDto != null)
		{
			final StPayThreeDSResponseStatus responseStatus = threeDSResponseDto.getResponseStatus();
			if(responseStatus.getResponseCode() == ThreeDsConstants.RESP_CODE_SUCCESS)
			{
				// Mapping the data from object to another object , just to prevent from manual mapping
				response = objectMapper.convertValue(threeDSResponseDto.getResponsePayload(), ThreeDSResponse.class);
				response.setGpasKey(gpasGuid);
			}
			response.setMerchantReferenceId(merchantReferenceID);
			response.setResponseCode(responseStatus.getResponseCode());
			response.setResponseDescription(responseStatus.getResponseDescription());
		}
		response.setRequestSentToStpayDatetime(stPayResponseWrapper.getTimeSent());
		response.setRespRcvdFromStpayDatetime(stPayResponseWrapper.getTimeReceived());
		return response;
	}
}