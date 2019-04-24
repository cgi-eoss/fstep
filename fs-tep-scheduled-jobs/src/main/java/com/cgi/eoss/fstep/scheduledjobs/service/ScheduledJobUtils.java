package com.cgi.eoss.fstep.scheduledjobs.service;

import java.time.OffsetDateTime;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class ScheduledJobUtils {

	private ScheduledJobUtils() {

	}

	public static Set<String> getCronExpressionsEveryMonthFromStart(OffsetDateTime start) {
		int day = start.getDayOfMonth();
		if (day <= 28) {
	        return ImmutableSet.of(String.format("%d %d %d %d JAN-DEC ? *", start.getSecond(), start.getMinute(), start.getHour(), day));
	    }
	    if (day == 29 || day == 30) {
	        return ImmutableSet.of("0 0 0 L FEB ? *", String.format("%d %d %d %d JAN,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC ? *", start.getSecond(), start.getMinute(), start.getHour(), day));
	    }
	    // day == 31
	    return ImmutableSet.of(String.format("%d %d %d L * ? *", start.getSecond(), start.getMinute(), start.getHour()));
	}

	public static String getCronExpressionEveryHourFromStart(OffsetDateTime start) {
		return String.format("%d %d * ? * * *", start.getSecond(), start.getMinute());
	}

}
