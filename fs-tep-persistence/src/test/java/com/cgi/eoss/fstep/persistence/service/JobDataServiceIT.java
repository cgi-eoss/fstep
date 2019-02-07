package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.Job.Status;
import com.cgi.eoss.fstep.model.JobConfig;
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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class JobDataServiceIT {

    @Rule
    public ExpectedException ex = ExpectedException.none();

    @Autowired
    private JobDataService dataService;
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
        Job job4 = new Job(jobConfig, UUID.randomUUID().toString(), owner2);
        dataService.save(ImmutableList.of(job1, job2, job3, job4));
        
        assertThat(dataService.getAll(), is(ImmutableList.of(job1, job2, job3, job4)));
        assertThat(dataService.countByOwnerAndStatusIn(owner, ImmutableList.of(Status.RUNNING)), is (1));
    }

    

}