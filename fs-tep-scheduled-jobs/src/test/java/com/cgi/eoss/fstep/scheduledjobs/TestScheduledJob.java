package com.cgi.eoss.fstep.scheduledjobs;

import java.util.Map;

import com.cgi.eoss.fstep.scheduledjobs.service.ScheduledJob;

public class TestScheduledJob extends ScheduledJob{

	@Override
	public void executeJob(Map<String, Object> jobContext) {
		//No-Op
	}

}
