package com.cgi.eoss.fstep.scheduledjobs;

import java.util.Map;

import com.cgi.eoss.fstep.scheduledjobs.service.ScheduledJob;
import org.springframework.beans.factory.annotation.Autowired;

public class TestScheduledJob extends ScheduledJob{

    @Autowired
    private CalleeStub calleeStub;
    
	@Override
	public void executeJob(Map<String, Object> jobContext) {
		calleeStub.callMe();
	}

}
