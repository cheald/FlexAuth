package com.chrisheald.flexauth;

class TokenUtils {
	static String getEnrollHost(String region)
	{
		if(region.toLowerCase().equals("cn"))
		{
			return "http://mobile-service.battlenet.com.cn";
		}
		else
		{
			return "http://mobile-service.blizzard.com";
		}
	}
	
	static String getEnrollUri(String region)
	{
		return getEnrollHost(region) + "/enrollment/enroll.htm";
	}
	
	static String getSyncUri(String region)
	{
		return getEnrollHost(region) + "/enrollment/time.htm";
	}
	
	static String getRestoreUri(String region)
	{
		return getEnrollHost(region) + "/enrollment/initiatePaperRestore.htm";
	}
	
	static String getRestoreValidateUri(String region)
	{
		return getEnrollHost(region) + "/enrollment/validatePaperRestore.htm";
	}
	
	static boolean isValidSerial(String serial)
	{
		if(serial.startsWith("US-") || serial.startsWith("EU-") || serial.startsWith("CN-") || serial.startsWith("KR-"))
		return true;
		else return false;
	}
	
	static boolean isValidRegion(String region)
	{
		if(region.equals("US") || region.equals("EU") || region.equals("CN") || region.equals("KR"))
		return true;
		else return false;
	}
}
