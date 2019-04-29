package com.cgi.eoss.fstep.scheduledjobs;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.scheduledjobs.service.ScheduledJobService;
import com.cgi.eoss.fstep.scheduledjobs.service.ScheduledJobUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ScheduledJobsConfig.class})
@TestPropertySource("classpath:test-scheduled-jobs.properties")
@Transactional
public class ScheduledJobsServiceIT {
    
	@Autowired
    private ScheduledJobService scheduledJobService;
   

    @Test
    public void test() {
    	Map<String, Object> jobContext = new HashMap<>();
		jobContext.put("testData", "testData");
		OffsetDateTime now = OffsetDateTime.now();
		scheduledJobService.scheduleJob(TestScheduledJob.class, "test", jobContext, ScheduledJobUtils.getCronExpressionEveryMonthFromStart(now), new Date(now.plusDays(1).toInstant().toEpochMilli()));
		Date nextScheduledDate = scheduledJobService.getNextScheduledTime("test");
		OffsetDateTime nextScheduledTime = nextScheduledDate.toInstant().atOffset(now.getOffset());
		assertThat(Duration.between(now, nextScheduledTime).toDays() <= 31, is(true));
    }


	
   
}