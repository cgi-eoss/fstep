package com.cgi.eoss.fstep.scheduledjobs.service;

import java.util.Date;
import java.util.List;
import java.util.Map;


public interface ScheduledJobService {
	
	public void scheduleJob(Class<? extends ScheduledJob> jobClass, String identity, Map<String, Object> jobContext, String cronExpression);

	public void scheduleJob(Class<? extends ScheduledJob> jobClass, String identity, Map<String, Object> jobContext,
			String cronExpression, Date startDateTime);

	public void unscheduleJob(String identity);

	Date getNextScheduledTime(String identity);

	List<Date> getNextScheduledTimes(int number, String identity);

}
