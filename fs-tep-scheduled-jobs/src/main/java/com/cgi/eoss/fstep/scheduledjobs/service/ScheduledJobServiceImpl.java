package com.cgi.eoss.fstep.scheduledjobs.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ScheduledJobServiceImpl implements ScheduledJobService{

	
	private Scheduler scheduler;

	@Autowired
	public ScheduledJobServiceImpl(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
	
	@Override
	public void scheduleJob(Class<? extends ScheduledJob> jobClass, String identity, Map<String, Object> jobContext, String cronExpression) {
		scheduleJob(jobClass, identity, jobContext, cronExpression, null);
	}
	
	@Override
	public void scheduleJob(Class<? extends ScheduledJob> jobClass, String identity, Map<String, Object> jobContext, String cronExpression, Date startDateTime) {
		JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.putAll(jobContext);
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .storeDurably(true)
                .usingJobData(jobDataMap)
                .build();
        TriggerBuilder<CronTrigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(identity, "fstep-triggers")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionIgnoreMisfires());
        if (startDateTime != null) {
        	triggerBuilder.startAt(startDateTime);
        }
         CronTrigger cronTrigger = triggerBuilder.build();
        try {
			scheduler.scheduleJob(jobDetail, cronTrigger);
		} catch (SchedulerException e) {
			throw new ScheduledJobException(e);
		}
	}


	@Override
	public void unscheduleJob(String identity) {
		try {
			scheduler.unscheduleJob(TriggerKey.triggerKey(identity, "fstep-triggers"));
		} catch (SchedulerException e) {
			throw new ScheduledJobException(e);
		}
		
	}
	
	@Override
	public Date getNextScheduledTime(String identity) {
		try {
			Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(identity, "fstep-triggers"));
			return trigger.getNextFireTime();
		} catch (SchedulerException e) {
			throw new ScheduledJobException(e);
		}
		
	}
	
	@Override
	public List<Date> getNextScheduledTimes(int number, String identity) {
		try {
			List<Date> nextFireTimes = new ArrayList<>();
			Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(identity, "fstep-triggers"));
			Date nextFireTime = trigger.getNextFireTime();
			nextFireTimes.add(nextFireTime);
			for (int i = 0; i < number - 1; i++) {
				nextFireTime = trigger.getFireTimeAfter(nextFireTime);
				nextFireTimes.add(nextFireTime);
			}
			return nextFireTimes;
		} catch (SchedulerException e) {
			throw new ScheduledJobException(e);
		}
		
	}
}
