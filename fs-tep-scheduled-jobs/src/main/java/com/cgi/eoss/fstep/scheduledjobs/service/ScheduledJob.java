package com.cgi.eoss.fstep.scheduledjobs.service;

import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class ScheduledJob extends QuartzJobBean{

    
    @Override
    protected final void executeInternal(JobExecutionContext context) throws JobExecutionException {
    	LOG.debug("Scheduled job execution");
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        executeJob(jobDataMap);
    }
    
    public abstract void executeJob(Map<String, Object> jobContext);
    
}

