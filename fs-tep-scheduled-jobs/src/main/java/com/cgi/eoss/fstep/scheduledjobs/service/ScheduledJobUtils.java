package com.cgi.eoss.fstep.scheduledjobs.service;

import java.time.OffsetDateTime;

public class ScheduledJobUtils {
	
	private ScheduledJobUtils() {
		
	}
	
	public static String getCronExpressionEveryMonthFromStart(OffsetDateTime start) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(start.getSecond());
		stringBuilder.append(" ");
		stringBuilder.append(start.getMinute());
		stringBuilder.append(" ");
		stringBuilder.append(start.getHour());
		stringBuilder.append(" ");
		stringBuilder.append(start.getDayOfMonth());
		stringBuilder.append(" ");
		stringBuilder.append("*");
		stringBuilder.append(" ");
		stringBuilder.append("?");
		stringBuilder.append(" ");
		stringBuilder.append("*");
		return stringBuilder.toString();
	}

}
