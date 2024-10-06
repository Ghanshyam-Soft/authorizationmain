package com.staples.payment.authorization.constant;

/*
 * Only values from CCS code is "Active" or "Inactive" but it does not use an enum or any constant on that side.
 * Thus I decided to it use a series of string constants instead of an enum.
 */
public class CardConsentStatus
{
	public static final String Active = "Active";
	public static final String Inactive = "Inactive";
}