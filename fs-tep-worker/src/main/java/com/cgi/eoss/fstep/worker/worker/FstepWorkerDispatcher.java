package com.cgi.eoss.fstep.worker.worker;

import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.rpc.worker.ContainerExit;
import com.cgi.eoss.fstep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.fstep.rpc.worker.ExitParams;
import com.cgi.eoss.fstep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc.FstepWorkerBlockingStub;
import com.cgi.eoss.fstep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.fstep.rpc.worker.JobEnvironment;
import com.cgi.eoss.fstep.rpc.worker.JobError;
import com.cgi.eoss.fstep.rpc.worker.JobEvent;
import com.cgi.eoss.fstep.rpc.worker.JobEventType;
import com.cgi.eoss.fstep.rpc.worker.JobInputs;
import com.cgi.eoss.fstep.rpc.worker.JobSpec;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Service for executing FS-TEP (WPS) services inside Docker containers.
 * </p>
 */
@Log4j2
@Service
public class FstepWorkerDispatcher {

    private FstepQueueService queueService;

    private WorkerLocator workerLocator;
    
    private String workerId;

    private FstepWorkerNodeManager nodeManager;

    private static final long QUEUE_SCHEDULER_INTERVAL_MS = 10L * 1000L;

    @Autowired
    public FstepWorkerDispatcher(FstepQueueService queueService, WorkerLocator workerLocator, @Qualifier("workerId") String workerId, FstepWorkerNodeManager nodeManager) {
        this.queueService = queueService;
        this.workerLocator = workerLocator;
        this.workerId = workerId;
        this.nodeManager = nodeManager;

    }


    @Scheduled(fixedRate = QUEUE_SCHEDULER_INTERVAL_MS, initialDelay = 10000L)
    public void getNewJobs() {
        while (nodeManager.hasCapacity()) {
            LOG.debug("Checking for available jobs in the queue");
            JobSpec nextJobSpec = (JobSpec) queueService.receiveObjectWithTimeout(FstepQueueService.jobQueueName, 100);
            if (nextJobSpec != null) {
                LOG.info("Dequeued job {}", nextJobSpec.getJob().getId());
                nodeManager.reserveNodeForJob(nextJobSpec.getJob().getId());
                Thread t = new Thread(new JobExecutor(nextJobSpec, queueService));
                t.start();
            } else {
                LOG.debug("Job queue currently empty");
            }
        }
    }

    public interface JobUpdateListener {

        void jobUpdate(Object object);

    }

    @Data
    public class JobExecutor implements Runnable, JobUpdateListener {

        Map<String, Object> messageHeaders;
        private final JobSpec jobSpec;
        private final FstepQueueService queueService;

        @Override
        public void run() {
            messageHeaders = new HashMap<>();
            messageHeaders.put("workerId", workerId);
            messageHeaders.put("jobId", jobSpec.getJob().getIntJobId());
            executeJob(jobSpec, this);

        }


        @Override
        public void jobUpdate(Object object) {
            queueService.sendObject(FstepQueueService.jobUpdatesQueueName, messageHeaders, object);
        }

    }


    // Entry point after Job is dequeued
    private void executeJob(JobSpec jobSpec, JobUpdateListener jobUpdateListener) {

        try {
            FstepWorkerBlockingStub rpcWorker = workerLocator.getWorkerById(workerId);
            jobUpdateListener.jobUpdate(JobEvent.newBuilder().setJobEventType(JobEventType.DATA_FETCHING_STARTED).build());
            JobInputs jobInputs = JobInputs.newBuilder().setJob(jobSpec.getJob()).addAllInputs(jobSpec.getInputsList()).build();
            JobEnvironment jobEnvironment = rpcWorker.prepareInputs(jobInputs);
            jobUpdateListener.jobUpdate(JobEvent.newBuilder().setJobEventType(JobEventType.DATA_FETCHING_COMPLETED).build());

            List<String> ports = new ArrayList<String>();
            ports.addAll(jobSpec.getExposedPortsList());

            List<String> binds = new ArrayList<String>();
            binds.add("/data:/data:ro");// TODO Do not bind everything, just the required folder (can be derived)
            binds.add(jobEnvironment.getWorkingDir() + "/FSTEP-WPS-INPUT.properties:"
                    + "/home/worker/workDir/FSTEP-WPS-INPUT.properties:ro");
            binds.add(jobEnvironment.getInputDir() + ":" + "/home/worker/workDir/inDir:ro");
            binds.add(jobEnvironment.getOutputDir() + ":" + "/home/worker/workDir/outDir:rw");
            binds.addAll(jobSpec.getUserBindsList());
            JobDockerConfig request =
                    JobDockerConfig.newBuilder().setJob(jobSpec.getJob()).setServiceName(jobSpec.getService().getName())
                            .setDockerImage(jobSpec.getService().getDockerImageTag()).addAllBinds(binds).addAllPorts(ports).build();
            rpcWorker.launchContainer(request);
            jobUpdateListener.jobUpdate(JobEvent.newBuilder().setJobEventType(JobEventType.PROCESSING_STARTED).build());
            int exitCode;
            if (jobSpec.getHasTimeout()) {
                ExitWithTimeoutParams exitRequest =
                        ExitWithTimeoutParams.newBuilder().setJob(jobSpec.getJob()).setTimeout(jobSpec.getTimeoutValue()).build();
                ContainerExitCode containerExitCode = rpcWorker.waitForContainerExitWithTimeout(exitRequest);
                exitCode = containerExitCode.getExitCode();
            } else {
                ExitParams exitRequest = ExitParams.newBuilder().setJob(jobSpec.getJob()).build();
                ContainerExitCode containerExitCode = rpcWorker.waitForContainerExit(exitRequest);
                exitCode = containerExitCode.getExitCode();
            }
            nodeManager.releaseJobNode(jobSpec.getJob().getId());
            jobUpdateListener.jobUpdate(ContainerExit.newBuilder().setExitCode(exitCode).setJobEnvironment(jobEnvironment).build());
        } catch (Exception e) {
            nodeManager.releaseJobNode(jobSpec.getJob().getId());
            jobUpdateListener.jobUpdate(JobError.newBuilder().setErrorDescription(e.getMessage()).build());

        }
    }

}
