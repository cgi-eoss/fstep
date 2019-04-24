package com.cgi.eoss.fstep.scheduledjobs.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
public interface ScheduledJobService {
	
	public void scheduleCronJob(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext, String cronExpression, boolean ignoreMisfires);

	public void scheduleCronJob(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext,
			String cronExpression, Date startDateTime, boolean ignoreMisfires);
	
	public void scheduleCronsJob(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext,
			Set<String> cronExpression, Date startDateTime, boolean ignoreMisfires);
	
	public void scheduleJobEveryNSeconds(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext,
	                int seconds);
	
	public void scheduleJobEveryNSeconds(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext,
                    int seconds, int count);
    
	
	public void unscheduleJob(String identity, String group);
	
	public void deleteJob(String identity, String group);

	Date getNextScheduledTime(String identity, String group);

	List<Date> getNextScheduledTimes(int number, String identity, String group);

    public void pauseJob(String systematicProcessingIdentity, String systematicProcessingGroup);
    
    public void resumeJob(String systematicProcessingIdentity, String systematicProcessingGroup);

}
