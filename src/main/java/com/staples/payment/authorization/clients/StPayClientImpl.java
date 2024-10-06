package com.staples.payment.authorization.clients;

import java.net.URI;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.staples.pay.client.RequestHeaderConfig;
import com.staples.pay.client.STPayClientRequestHeaderUtil;
import com.staples.pay.client.STPayClientRequestHeaderUtilImpl;
import com.staples.payment.authorization.configuration.properties.StaplesPayConfig;
import com.staples.payment.authorization.dto.staplespay.StPayAuthToken;
import com.staples.payment.authorization.dto.staplespay.StPayThreeDSRequest;
import com.staples.payment.authorization.dto.staplespay.StPayThreeDSResponse;
import com.staples.payment.shared.aci.response.ResponseWrapper;

@Service
public class StPayClientImpl implements StPayClient
{
	private final WebClient webClient;

	private final StaplesPayConfig staplesPayConfig;

	public StPayClientImpl(StaplesPayConfig staplesPayConfig)
	{
		webClient = WebClient.builder().build();

		this.staplesPayConfig = staplesPayConfig;
	}

	@Override
	public ResponseWrapper<StPayAuthToken> getAuthToken(StPayThreeDSRequest tokenRequest)
	{
		final String hashToken = createHashToken();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("STPAY_API_AUTH_TOKEN", hashToken);

		ResponseWrapper<StPayAuthToken> tokenResponse = this.makeStaplesPayCall(tokenRequest, staplesPayConfig.getAuthTokenUrl(), headers, StPayAuthToken.class);
		return tokenResponse;
	}

	@Override
	public ResponseWrapper<StPayThreeDSResponse> get3DSResponse(StPayThreeDSRequest threeDSRequest, String authToken)
	{
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("PAYMENT_AUTH_TOKEN", staplesPayConfig.getMerchantId() + ":" + authToken);

		ResponseWrapper<StPayThreeDSResponse> threeDSResponse = this.makeStaplesPayCall(threeDSRequest, staplesPayConfig.getThreeDSUrl(), headers, StPayThreeDSResponse.class);
		return threeDSResponse;
	}

	private <T> ResponseWrapper<T> makeStaplesPayCall(Object request, URI uri, HttpHeaders headers, Class<T> responseClass)
	{
		Instant timeSent = Instant.now();

		ResponseWrapper<T> response = webClient.post()
				.uri(uri)
				.headers(httpHeaders -> headers.forEach((key, value) -> httpHeaders.set(key, value.get(0))))
				.bodyValue(request)
				.exchangeToMono(clientResponse -> clientResponse.toEntity(responseClass))
				.map(entity -> new ResponseWrapper<T>(
						null,
						entity != null ? entity.getStatusCode() : null,
						timeSent,
						Instant.now(),
						entity != null ? entity.getBody() : null))
				.block(staplesPayConfig.getTimeout());

		return response;
	}

	private String createHashToken()
	{
		String token = null;
		try
		{
			RequestHeaderConfig requestHeader = new RequestHeaderConfig(staplesPayConfig.getMerchantId(), staplesPayConfig.getAuthSecret(), staplesPayConfig.getAuthKey());
			STPayClientRequestHeaderUtil headerUtil = new STPayClientRequestHeaderUtilImpl(requestHeader);
			token = headerUtil.generateAPIRequestHeaderHash();
		}
		catch(Exception e)
		{
			throw new RuntimeException("HashToken creation failure: {}", e);
		}
		return token;
	}
}
