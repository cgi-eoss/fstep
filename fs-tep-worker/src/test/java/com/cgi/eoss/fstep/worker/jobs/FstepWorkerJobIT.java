package com.cgi.eoss.fstep.worker.jobs;

import static org.junit.Assume.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cgi.eoss.fstep.worker.WorkerConfig;
import com.cgi.eoss.fstep.worker.WorkerTestConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {WorkerConfig.class, WorkerTestConfig.class})
@TestPropertySource("classpath:test-worker.properties")
public class FstepWorkerJobIT {

	@Autowired
	WorkerJobDataService jobDataService;
	
	@BeforeClass
    public static void precondition() {
        // Shortcut if docker socket is not accessible to the current user
        assumeTrue("Unable to write to Docker socket; disabling docker tests", Files.isWritable(Paths.get("/var/run/docker.sock")));
        // TODO Pass in a DOCKER_HOST env var to allow remote docker engine use
    }
    @Test
    public void test() {
        WorkerJob j = new WorkerJob(UUID.randomUUID().toString(), "54");
        jobDataService.save(j);
    }


}
