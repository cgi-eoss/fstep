package com.cgi.eoss.fstep.scheduledjobs.service;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.PersistJobDataAfterExecution;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public abstract class PersistentScheduledJob extends ScheduledJob{

    
}

