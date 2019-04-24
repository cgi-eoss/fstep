package com.cgi.eoss.fstep.scheduledjobs;

import com.cgi.eoss.fstep.scheduledjobs.service.PersistentScheduledJob;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
public class TestPersistentScheduledJob extends PersistentScheduledJob{

    @Autowired
    private CalleeStub calleeStub;
    
	@Override
	public void executeJob(Map<String, Object> jobContext) {
	    Integer count = (Integer) jobContext.get("count");
	    if (count == null) {
	        count = 0;
	    }
	    count++;
	    jobContext.put("count", count);
	    if (count == 2) {
	        calleeStub.callMe(count);
	    }
	}

}
