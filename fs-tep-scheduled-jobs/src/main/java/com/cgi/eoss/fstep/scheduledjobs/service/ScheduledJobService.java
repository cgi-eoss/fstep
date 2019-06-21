package com.cgi.eoss.fstep.scheduledjobs.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface ScheduledJobService {
	
	@Transactional
	public void scheduleCronJob(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext, String cronExpression, boolean ignoreMisfires);

	@Transactional
	public void scheduleCronJob(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext,
			String cronExpression, Date startDateTime, boolean ignoreMisfires);
	
	@Transactional
	public void scheduleCronsJob(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext,
			Set<String> cronExpression, Date startDateTime, boolean ignoreMisfires);
	
	@Transactional
	public void scheduleJobEveryNSeconds(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext,
	                int seconds);
	
	@Transactional
	public void scheduleJobEveryNSeconds(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext,
                    int seconds, int count);
    
	@Transactional
	public void unscheduleJob(String identity, String group);
	
	@Transactional
	public void deleteJob(String identity, String group);

	Date getNextScheduledTime(String identity, String group);

	List<Date> getNextScheduledTimes(int number, String identity, String group);

	@Transactional
    public void pauseJob(String systematicProcessingIdentity, String systematicProcessingGroup);
	
	@Transactional
    public void resumeJob(String systematicProcessingIdentity, String systematicProcessingGroup);

}
