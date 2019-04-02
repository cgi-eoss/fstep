package com.cgi.eoss.fstep.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.api.ApiConfig;
import com.cgi.eoss.fstep.api.ApiTestConfig;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.JobProcessing;
import com.cgi.eoss.fstep.model.Role;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.service.JobConfigDataService;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.persistence.service.JobProcessingDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
import com.cgi.eoss.fstep.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ReportsApiIT {

	@Autowired 
	private ServiceDataService serviceDataService;
	
	@Autowired
    private JobConfigDataService jobConfigDataService;
	
    @Autowired
    private JobDataService jobDataService;

    @Autowired
    private JobProcessingDataService jobProcessingDataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User fstepUser;
    private User fstepExpertUser;
    private User fstepContentAuthority;
    private FstepService svc;

    @Before
    public void setUp() {
        fstepUser = new User("fstep-user");
        fstepUser.setRole(Role.USER);
        fstepExpertUser = new User("fstep-expert-user");
        fstepExpertUser.setRole(Role.EXPERT_USER);
        fstepContentAuthority = new User("fstep-content-authority");
        fstepContentAuthority.setRole(Role.CONTENT_AUTHORITY);

        userDataService.save(ImmutableSet.of(fstepUser, fstepExpertUser, fstepContentAuthority));

        svc = new FstepService("service-1", fstepExpertUser, "dockerTag");
        svc.setStatus(FstepService.Status.AVAILABLE);
        serviceDataService.save(svc);
        JobConfig jobConfig = new JobConfig(fstepUser, svc);
        jobConfigDataService.save(jobConfig);
        Job job = new Job(jobConfig, UUID.randomUUID().toString(), fstepUser);
        job.setStartTime(LocalDateTime.now().minusDays(2));
        job.setEndTime(LocalDateTime.now().minusMinutes(4));
        jobDataService.save(job);
        JobProcessing jobProcessing = jobProcessingDataService.buildNew(job);
        jobProcessing.setStartProcessingTime(OffsetDateTime.now().minusMinutes(20));
        jobProcessing.setEndProcessingTime(OffsetDateTime.now().minusMinutes(20));
        jobProcessingDataService.save(jobProcessing);
    }

    

    @Test
    public void testJobsReport() throws Exception {
    	mockMvc.perform(get("/api/reports/jobs/CSV")
        		.param("startDateTime", "2019-04-01T00:00:00Z")
        		.param("endDateTime", "2019-04-30T00:00:00Z")
        		.header("REMOTE_USER", fstepUser.getName()))
                .andExpect(status().isOk());
    }

}