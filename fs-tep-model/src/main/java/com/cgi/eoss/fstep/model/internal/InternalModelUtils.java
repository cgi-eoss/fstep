package com.cgi.eoss.fstep.model.internal;

public class InternalModelUtils {

	public static String platformTimeRegexpToGeoserverTimeRegexp(String platformTimeRegexp) {
		if (platformTimeRegexp == null) {
	        return null;
	    }
		int startEndIndex = platformTimeRegexp.indexOf("?<startEnd>");
		if (startEndIndex != -1) {
			return capturingGroup(platformTimeRegexp, startEndIndex, "?<startEnd>");
		}
		int startIndex = platformTimeRegexp.indexOf("?<start>");
		if (startIndex != -1) {
			return capturingGroup(platformTimeRegexp, startIndex, "?<start>");
		}
		int endIndex = platformTimeRegexp.indexOf("?<end>");
		if (endIndex != -1) {
			return capturingGroup(platformTimeRegexp, endIndex, "?<end>");
		}
		throw new IllegalArgumentException("platformTimeRegexp not recognized");
	}
	
	private static String capturingGroup(String platformRegexp, int startEndIndex, String capturingGroup) {
		int openingParenthesisIndex = platformRegexp.substring(0, startEndIndex).lastIndexOf('(') + capturingGroup.length() + 1;
		int closingParenthesisIndex = platformRegexp.substring(startEndIndex).indexOf(')') + startEndIndex;
		return "(" + platformRegexp.substring(openingParenthesisIndex, closingParenthesisIndex) + ")";
	}
}
