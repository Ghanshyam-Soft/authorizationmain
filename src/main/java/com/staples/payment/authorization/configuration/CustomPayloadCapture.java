package com.staples.payment.authorization.configuration;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.filter.CommonsRequestLoggingFilter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomPayloadCapture extends CommonsRequestLoggingFilter
{
	private String excludedChars;

	public CustomPayloadCapture(String excludedChars)
	{
		super();
		this.excludedChars = excludedChars;
	}

	@Override
	protected boolean shouldLog(HttpServletRequest request)
	{
		return true;
	}

	@Override
	protected void afterRequest(HttpServletRequest request, String message)
	{
		if(log.isDebugEnabled() && !request.getRequestURI().contains("health"))
		{
			log.debug(message.replaceAll(excludedChars, ""));
		}
	}

	@Override
	protected void beforeRequest(HttpServletRequest request, String message)
	{
		if(log.isDebugEnabled() && !request.getRequestURI().contains("health"))
		{
			log.debug(message.replaceAll(excludedChars, ""));
		}
	}
}