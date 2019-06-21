package com.cgi.eoss.fstep.scheduledjobs.service;

import com.google.common.collect.ImmutableSet;
import lombok.extern.log4j.Log4j2;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Log4j2
public class ScheduledJobServiceImpl implements ScheduledJobService{

	
	private Scheduler scheduler;

	@Autowired
	public ScheduledJobServiceImpl(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
	
	@Override
	@Transactional
	public void scheduleCronJob(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext, String cronExpression, boolean ignoreMisfires) {
		scheduleCronJob(jobClass, identity, group, jobContext, cronExpression, null, ignoreMisfires);
	}
	
	@Override
	@Transactional
	public void scheduleCronJob(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext, String cronExpression, Date startDateTime, boolean ignoreMisfires) {
		scheduleCronsJob(jobClass, identity, group, jobContext, ImmutableSet.of(cronExpression), startDateTime, ignoreMisfires);
	}
	
	@Override
	@Transactional
	public void scheduleCronsJob(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext, Set<String> cronExpressions, Date startDateTime, boolean ignoreMisfires) {
	    JobDetail jobDetail = createJobDetail(jobClass, identity, group, jobContext);
        int index = 0;
        Set<Trigger> triggers = new HashSet<>();
        for (String cronExpression: cronExpressions) {
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
            if (ignoreMisfires) {
                cronScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            }
            else {
                cronScheduleBuilder.withMisfireHandlingInstructionDoNothing();
            }
            TriggerBuilder<CronTrigger> triggerBuilder = TriggerBuilder.newTrigger().forJob(jobDetail).withIdentity(identity+ "-" + index++, group)
                    .withSchedule(cronScheduleBuilder);
        	if (startDateTime != null) {
            	triggerBuilder.startAt(startDateTime);
            }
        	triggers.add(triggerBuilder.build());
        }
        try {
            scheduler.scheduleJob(jobDetail, triggers, false);
		} catch (SchedulerException e) {
			throw new ScheduledJobException(e);
		}
	}

	@Override
	@Transactional
	public void scheduleJobEveryNSeconds(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext, int seconds) {
	    scheduleJobEveryNSeconds(jobClass, identity, group, jobContext, seconds, -1);
	}
	
	@Override
	@Transactional
    public void scheduleJobEveryNSeconds(Class<? extends ScheduledJob> jobClass, String identity, String group, Map<String, Object> jobContext, int seconds, int count) {
        JobDetail jobDetail = createJobDetail(jobClass, identity, group, jobContext);
        SimpleScheduleBuilder scheduleBuilder;
        if (count == -1) {
            scheduleBuilder = SimpleScheduleBuilder.repeatSecondlyForever(seconds);
        }
        else {
            scheduleBuilder = SimpleScheduleBuilder.repeatSecondlyForTotalCount(count, seconds);
        }
        scheduleBuilder = scheduleBuilder.withMisfireHandlingInstructionNextWithExistingCount();
        TriggerBuilder<SimpleTrigger> triggerBuilder = TriggerBuilder.newTrigger().withIdentity(identity, group).
                        withSchedule(scheduleBuilder);
        try {
            scheduler.scheduleJob(jobDetail, triggerBuilder.build());
        } catch (SchedulerException e) {
            throw new ScheduledJobException(e);
        }
    }

    private JobDetail createJobDetail(Class<? extends ScheduledJob> jobClass, String identity, String group,
                    Map<String, Object> jobContext) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.putAll(jobContext);
        return JobBuilder.newJob(jobClass)
                .withIdentity(JobKey.jobKey(identity, group))
                .storeDurably(true)
                .usingJobData(jobDataMap)
                .build();
    }

	@Override
	@Transactional
	public void unscheduleJob(String identity, String group) {
		try {
		    JobKey jobKey = JobKey.jobKey(identity, group);
            if (scheduler.checkExists(jobKey)) {
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(JobKey.jobKey(identity, group));
    			for (Trigger trigger: triggers) {
    				scheduler.unscheduleJob(trigger.getKey());
    			}
    			return;
    		}
            TriggerKey triggerKey = TriggerKey.triggerKey(identity, group);
            if (scheduler.checkExists(triggerKey)) {
                scheduler.unscheduleJob(triggerKey);
            }
            
		} catch (SchedulerException e) {
			throw new ScheduledJobException(e);
		}
	}
	
	@Override
	public Date getNextScheduledTime(String identity, String group) {
		try {
			List<? extends Trigger> triggers = scheduler.getTriggersOfJob(JobKey.jobKey(identity, group));
			Date nextScheduledTime = null;
			for (Trigger trigger: triggers) {
				if (nextScheduledTime == null || nextScheduledTime.after(trigger.getNextFireTime())){
					nextScheduledTime = trigger.getNextFireTime();
				}
				
			}
			return nextScheduledTime;
		} catch (SchedulerException e) {
			throw new ScheduledJobException(e);
		}
		
	}
	
	@Override
	@Transactional
    public void deleteJob(String identity, String group) {
        try {
            scheduler.deleteJob(JobKey.jobKey(identity, group));
        } catch (SchedulerException e) {
            throw new ScheduledJobException(e);
        }
    }
	
	@Override
	public List<Date> getNextScheduledTimes(int number, String identity, String group) {
		try {
			List<? extends Trigger> triggers = scheduler.getTriggersOfJob(JobKey.jobKey(identity, group));
			List<Date> nextDatesForAllTriggers = new ArrayList<>();
			for (Trigger trigger: triggers) {
				Date nextFireTime = trigger.getNextFireTime();
				if (nextFireTime != null) {
					nextDatesForAllTriggers.add(nextFireTime);
					for (int i = 0; i < number - 1; i++) {
						nextFireTime = trigger.getFireTimeAfter(nextFireTime);
						if (nextFireTime == null) {
							continue;
						}
						nextDatesForAllTriggers.add(nextFireTime);
					}
				}
			}
			Collections.sort(nextDatesForAllTriggers);
			return nextDatesForAllTriggers.subList(0, number < nextDatesForAllTriggers.size() ? number : nextDatesForAllTriggers.size());
		} catch (SchedulerException e) {
			throw new ScheduledJobException(e);
		}
		
	}

    @Override
    @Transactional
    public void pauseJob(String identity, String group) {
        try {
            JobKey jobKey = JobKey.jobKey(identity, group);
            if (scheduler.checkExists(jobKey)) {
                scheduler.pauseJob(jobKey);
                return;
            }
            TriggerKey triggerKey = TriggerKey.triggerKey(identity, group);
            if (scheduler.checkExists(triggerKey)) {
                scheduler.pauseTrigger(triggerKey);
            }
        } catch (SchedulerException e) {
            throw new ScheduledJobException(e);
        }
        
    }
    
    @Override
    @Transactional
    public void resumeJob(String identity, String group) {
        try {
            JobKey jobKey = JobKey.jobKey(identity, group);
            if (scheduler.checkExists(jobKey)) {
                scheduler.resumeJob(jobKey);
                return;
            }
            TriggerKey triggerKey = TriggerKey.triggerKey(identity, group);
            if (scheduler.checkExists(triggerKey)) {
                scheduler.resumeTrigger(triggerKey);
            }
        } catch (SchedulerException e) {
            throw new ScheduledJobException(e);
        }
        
    }

   
}
