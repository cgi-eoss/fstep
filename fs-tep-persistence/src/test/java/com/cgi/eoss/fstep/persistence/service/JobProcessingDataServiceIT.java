package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.Job.Status;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.JobProcessing;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class JobProcessingDataServiceIT {

    @Rule
    public ExpectedException ex = ExpectedException.none();

    @Autowired
    private JobProcessingDataService dataService;
    
    @Autowired
    private JobDataService jobDataService;
    @Autowired
    private JobConfigDataService jobConfigService;
    @Autowired
    private UserDataService userService;
    @Autowired
    private ServiceDataService svcService;

    @Test
    public void test() throws Exception {
        User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        FstepService svc = new FstepService();
        svc.setName("Test Service");
        svc.setOwner(owner);
        svc.setDockerTag("dockerTag");
        FstepService svc2 = new FstepService();
        svc2.setName("Test Service2");
        svc2.setOwner(owner);
        svc2.setDockerTag("dockerTag");
        svcService.save(ImmutableSet.of(svc, svc2));

        Multimap<String, String> job1Inputs = ImmutableMultimap.of(
                "input1", "foo",
                "input2", "bar1",
                "input2", "bar2",
                "input3", "http://baz/?q=x,y&z={}"
        );
        JobConfig jobConfig = new JobConfig(owner, svc);
        jobConfig.setInputs(job1Inputs);
        JobConfig jobConfig2 = new JobConfig(owner, svc2);
        jobConfigService.save(ImmutableList.of(jobConfig, jobConfig2));

        Job job1 = new Job(jobConfig, UUID.randomUUID().toString(), owner);
        job1.setStatus(Status.RUNNING);
        Job job2 = new Job(jobConfig, UUID.randomUUID().toString(), owner);
        Job job3 = new Job(jobConfig, UUID.randomUUID().toString(), owner2);
        
        jobDataService.save(ImmutableList.of(job1, job2, job3));
        
        JobProcessing jobProcessing1 = new JobProcessing(job1, 1);
        JobProcessing jobProcessing2 = new JobProcessing(job1, 2);
        JobProcessing jobProcessing3 = new JobProcessing(job1, 3);
        dataService.save(ImmutableList.of(jobProcessing1, jobProcessing2, jobProcessing3));
        assertThat(dataService.findByJobAndMaxSequenceNum(job1).getSequenceNum(), is(3L));
        assertThat(dataService.findByJobOrderBySequenceNumAsc(job1).stream().map(p-> p.getId()).collect(Collectors.toList()), equalTo(ImmutableList.of(0L,1L,2L)));
        OffsetDateTime job2processingStart = OffsetDateTime.now();
        OffsetDateTime job2processingEnd = job2processingStart.plusMinutes(10);
        
        JobProcessing job2Processing1 = new JobProcessing(job2, 1);
        job2Processing1 = dataService.save(job2Processing1);
        job2Processing1.setEndProcessingTime(job2processingEnd);
        job2Processing1 = dataService.save(job2Processing1);
        assertThat(dataService.findByJobAndMaxSequenceNum(job2).getEndProcessingTime(), is(job2processingEnd));
        
    }

    

}