package com.cgi.eoss.fstep.orchestrator.service;

import static com.google.common.collect.Multimaps.toMultimap;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.jooq.lambda.Unchecked;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.fstep.catalogue.util.GeoUtil;
import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.logging.Logging;
import com.cgi.eoss.fstep.model.Databasket;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.Job.Status;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.JobStep;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.internal.OutputProductMetadata;
import com.cgi.eoss.fstep.persistence.service.DatabasketDataService;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.rpc.CancelJobParams;
import com.cgi.eoss.fstep.rpc.CancelJobResponse;
import com.cgi.eoss.fstep.rpc.FstepJobLauncherGrpc;
import com.cgi.eoss.fstep.rpc.FstepServiceParams;
import com.cgi.eoss.fstep.rpc.FstepServiceResponse;
import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.JobParam;
import com.cgi.eoss.fstep.rpc.StopServiceParams;
import com.cgi.eoss.fstep.rpc.StopServiceResponse;
import com.cgi.eoss.fstep.rpc.worker.ContainerExit;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc.FstepWorkerBlockingStub;
import com.cgi.eoss.fstep.rpc.worker.GetOutputFileParam;
import com.cgi.eoss.fstep.rpc.worker.JobEnvironment;
import com.cgi.eoss.fstep.rpc.worker.JobError;
import com.cgi.eoss.fstep.rpc.worker.JobEvent;
import com.cgi.eoss.fstep.rpc.worker.JobEventType;
import com.cgi.eoss.fstep.rpc.worker.JobSpec;
import com.cgi.eoss.fstep.rpc.worker.ListOutputFilesParam;
import com.cgi.eoss.fstep.rpc.worker.OutputFileItem;
import com.cgi.eoss.fstep.rpc.worker.OutputFileList;
import com.cgi.eoss.fstep.rpc.worker.OutputFileResponse;
import com.cgi.eoss.fstep.rpc.worker.StopContainerResponse;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;

/**
 * <p>
 * Primary entry point for WPS services to launch in FS-TEP.
 * </p>
 * <p>
 * Provides access to FS-TEP data services and job distribution capability.
 * </p>
 */
@Service
@Log4j2
@GRpcService
public class FstepJobLauncher extends FstepJobLauncherGrpc.FstepJobLauncherImplBase {

    private static final String TIMEOUT_PARAM = "timeout";

    private static final int SINGLE_JOB_PRIORITY = 9;

    private final WorkerFactory workerFactory;
    private final JobDataService jobDataService;
    private final DatabasketDataService databasketDataService;
    private final FstepGuiServiceManager guiService;
    private final CatalogueService catalogueService;
    private final CostingService costingService;
    private final FstepSecurityService securityService;
    private FstepQueueService queueService;
    
    private Map<String, StreamObserver<FstepServiceResponse>> responseObservers = new HashMap<>();

    @Autowired
    public FstepJobLauncher(WorkerFactory workerFactory, JobDataService jobDataService,
            DatabasketDataService databasketDataService, FstepGuiServiceManager guiService,
            CatalogueService catalogueService, CostingService costingService,
            FstepSecurityService securityService, FstepQueueService queueService) {
        this.workerFactory = workerFactory;
        this.jobDataService = jobDataService;
        this.databasketDataService = databasketDataService;
        this.guiService = guiService;
        this.catalogueService = catalogueService;
        this.costingService = costingService;
        this.securityService = securityService;
        this.queueService = queueService;
    }

    @Override
    public void submitJob(FstepServiceParams request,
            StreamObserver<FstepServiceResponse> responseObserver) {
        String zooId = request.getJobId();
        String userId = request.getUserId();
        String serviceId = request.getServiceId();
        String jobConfigLabel = request.getJobConfigLabel();
        List<JobParam> rpcInputs = request.getInputsList();
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);

        Job job = null;
        com.cgi.eoss.fstep.rpc.Job rpcJob = null;
        try (CloseableThreadContext.Instance ctc =
                CloseableThreadContext.push("FS-TEP Service Orchestrator").put("userId", userId)
                        .put("serviceId", serviceId).put("zooId", zooId)) {
            // TODO Allow re-use of existing JobConfig
            job = jobDataService.buildNew(zooId, userId, serviceId, jobConfigLabel, inputs);
            rpcJob = GrpcUtil.toRpcJob(job);
            // Post back the job metadata for async responses
            responseObserver.onNext(FstepServiceResponse.newBuilder().setJob(rpcJob).build());

            ctc.put("jobId", String.valueOf(job.getId()));
            FstepService service = job.getConfig().getService();

            if (service.getType() == FstepService.Type.PARALLEL_PROCESSOR) {
                if (!checkInputs(job.getOwner(), rpcInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested inputs",
                                userId);
                    }
                    throw new ServiceExecutionException(
                            "User does not have read access to all requested inputs");
                }

                Collection<String> parallelInput = inputs.get("parallelInputs");
                List<String> newInputs = explodeParallelInput(parallelInput);

                if (!checkInputList(job.getOwner(), newInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested inputs",
                                userId);
                    }
                    throw new ServiceExecutionException(
                            "User does not have read access to all requested inputs");
                }
                responseObservers.put(job.getExtId(), responseObserver);
                List<Job> subJobs = createSubJobs(job, userId, service, newInputs, inputs);

                checkCost(job.getOwner(), job.getConfig());
                int i = 0;
                for (Job subJob : subJobs) {
                    chargeUser(subJob.getOwner(), subJob);
                    submitJob(subJob, GrpcUtil.toRpcJob(subJob),
                            GrpcUtil.mapToParams(subJob.getConfig().getInputs()), getJobPriority(i));
                    i++;
                }

            } 
            else {
                checkCost(job.getOwner(), job.getConfig());
                chargeUser(job.getOwner(), job);
                if (!checkInputs(job.getOwner(), rpcInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested inputs",
                                userId);
                    }
                    throw new ServiceExecutionException(
                            "User does not have read access to all requested inputs");
                }
                responseObservers.put(job.getExtId(), responseObserver);
                submitJob(job, rpcJob, rpcInputs, SINGLE_JOB_PRIORITY);
            }

        } catch (Exception e) {
            if (job != null) {
                job.setStatus(Job.Status.ERROR);
                job.setEndTime(LocalDateTime.now());
                jobDataService.save(job);
            }

            LOG.error("Failed to run processor - {}; notifying gRPC client", e.getMessage());
            responseObserver.onError(new StatusRuntimeException(
                    io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        }
    }

 
    private int getJobPriority(int messageNumber) {
        if (messageNumber >= 0 && messageNumber < 10) {
            return 6;
        }
        else if (messageNumber >= 10 && messageNumber < 30) {
            return 5;
        }
        else if (messageNumber >= 30 && messageNumber < 70) {
            return 4;
        }
        else if (messageNumber >= 70 && messageNumber < 150) {
            return 3;
        }
        else if (messageNumber >= 150 && messageNumber < 310) {
            return 2;
        }
        return 1;
    }

    private void checkCost(User user, JobConfig jobConfig) {
        int estimatedCost = costingService.estimateJobCost(jobConfig);
        if (estimatedCost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException(
                    "Estimated cost (" + estimatedCost + " coins) exceeds current wallet balance");
        }
    }

    private boolean checkInputs(User user, List<JobParam> inputsList) {
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(inputsList);

        Set<URI> inputUris = inputs.entries().stream().filter(e -> this.isValidUri(e.getValue()))
                .flatMap(e -> Arrays.stream(StringUtils.split(e.getValue(), ',')).map(URI::create))
                .collect(toSet());

        return inputUris.stream().allMatch(uri -> catalogueService.canUserRead(user, uri));
    }

    private boolean checkInputList(User user, List<String> inputsList) {
        return inputsList.stream().filter(e -> this.isValidUri(e)).map(URI::create).collect(toSet())
                .stream().allMatch(uri -> catalogueService.canUserRead(user, uri));
    }

    private boolean isValidUri(String test) {
        try {
            return URI.create(test).getScheme() != null;
        } catch (Exception unused) {
            return false;
        }
    }

    private void submitJob(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob, List<JobParam> rpcInputs, int priority)
            throws IOException {
        FstepService service = job.getConfig().getService();
        JobSpec.Builder jobSpecBuilder = JobSpec.newBuilder()
                .setService(GrpcUtil.toRpcService(service)).setJob(rpcJob).addAllInputs(rpcInputs);
        if (service.getType() == FstepService.Type.APPLICATION) {
            jobSpecBuilder.addExposedPorts(FstepGuiServiceManager.GUACAMOLE_PORT);
        }
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);

        if (inputs.containsKey(TIMEOUT_PARAM)) {
            int timeout = Integer.parseInt(Iterables.getOnlyElement(inputs.get(TIMEOUT_PARAM)));
            jobSpecBuilder = jobSpecBuilder.setHasTimeout(true).setTimeoutValue(timeout);
        }
        JobSpec jobSpec = jobSpecBuilder.build();
        HashMap<String, Object> messageHeaders = new HashMap<String, Object>();
        messageHeaders.put("jobId", job.getId());
        queueService.sendObject(FstepQueueService.jobQueueName, messageHeaders, jobSpec, priority);
    }

    @Override
    public void cancelJob(CancelJobParams request,
            StreamObserver<CancelJobResponse> responseObserver) {
        com.cgi.eoss.fstep.rpc.Job rpcJob = request.getJob();
        Job job = jobDataService.getById(Long.parseLong(rpcJob.getIntJobId()));
        Set<Job> subJobs = job.getSubJobs();
        if (subJobs.size() > 0) {
            for (Job subJob : subJobs) {
                if (subJob.getStatus() != Job.Status.CANCELLED)
                cancelJob(subJob);
            }
            //TODO Check if this implies parent is completed
        } else {
            if (job.getStatus() != Job.Status.CANCELLED) {
                cancelJob(job);
            }
        }
        responseObserver.onNext(CancelJobResponse.newBuilder().build());
        responseObserver.onCompleted();

    }

    private void cancelJob(Job job) {
        LOG.info("Cancelling job with id {}", job.getId());
        JobSpec queuedJobSpec = (JobSpec) queueService
                .receiveSelectedObject(FstepQueueService.jobQueueName, "jobId = " + job.getId());
        if (queuedJobSpec != null) {
            LOG.info("Refunding user for job id {}", job.getId());
            costingService.refundUser(job.getOwner().getWallet(), job);
            job.setStatus(Status.CANCELLED);
            jobDataService.save(job);
        }
    }
    
    @Override
    public void stopJob(StopServiceParams request,
            StreamObserver<StopServiceResponse> responseObserver) {
        com.cgi.eoss.fstep.rpc.Job rpcJob = request.getJob();
        try {
            Job job = jobDataService.getById(Long.parseLong(rpcJob.getIntJobId()));
            FstepWorkerGrpc.FstepWorkerBlockingStub worker =
                    workerFactory.getWorkerById(job.getWorkerId());
            if (worker == null)
                throw new IllegalStateException(
                        "FS-TEP worker not found for job " + rpcJob.getId());
            LOG.info("Stop requested for job {}", rpcJob.getId());
            StopContainerResponse stopContainerResponse = worker.stopContainer(rpcJob);
            LOG.info("Successfully stopped job {}", rpcJob.getId());
            responseObserver.onNext(StopServiceResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (

        Exception e) {
            LOG.error("Failed to stop job {} - message {}; notifying gRPC client", rpcJob.getId(),
                    e.getMessage());
            responseObserver.onError(new StatusRuntimeException(
                    io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        }
    }

    private List<String> explodeParallelInput(Collection<String> inputUris) {
        List<String> results = new ArrayList<String>();
        for (String inputUri : inputUris) {
            if (inputUri.startsWith("fstep://databasket")) {
                Databasket dataBasket = getDatabasketFromUri(inputUri);
                results.addAll(dataBasket.getFiles().stream().map(f -> f.getUri().toString())
                        .collect(toList()));
            } else {
                if (inputUri.contains((","))) {
                    results.addAll(Arrays.asList(inputUri.split(",")));
                } else {
                    results.add(inputUri);
                }
            }
        }
        return results;

    }

    private Databasket getDatabasketFromUri(String uri) {
        Matcher uriIdMatcher = Pattern.compile(".*/([0-9]+)$").matcher(uri);
        if (!uriIdMatcher.matches()) {
            throw new ServiceExecutionException("Failed to load databasket for URI: " + uri);
        }
        Long databasketId = Long.parseLong(uriIdMatcher.group(1));
        Databasket databasket = Optional.ofNullable(databasketDataService.getById(databasketId))
                .orElseThrow(() -> new ServiceExecutionException(
                        "Failed to load databasket for ID " + databasketId));
        LOG.debug("Listing databasket contents for id {}", databasketId);
        return databasket;
    }

    private List<Job> createSubJobs(Job parentJob, String userId, FstepService service,
            List<String> newInputs, Multimap<String, String> inputs) {
        List<Job> childJobs = new ArrayList<Job>();
        // Create the simpler map of parameters shared by all parallel jobs
        SetMultimap<String, String> sharedParams =
                MultimapBuilder.hashKeys().hashSetValues().build(inputs);
        sharedParams.removeAll("parallelInputs");
        for (String newInput : newInputs) {
            SetMultimap<String, String> parallelJobParams =
                    MultimapBuilder.hashKeys().hashSetValues().build(sharedParams);
            parallelJobParams.put("input", newInput);
            Job childJob =
                    jobDataService.buildNew(UUID.randomUUID().toString(), userId, service.getName(),
                            parentJob.getConfig().getLabel(), parallelJobParams, parentJob);
            parentJob.getSubJobs().add(childJob);
            childJobs.add(childJob);
        }
        parentJob.setParent(true);
        jobDataService.save(parentJob);
        return childJobs;
    }


    @JmsListener(destination = FstepQueueService.jobUpdatesQueueName)
    public void receiveJobUpdate(@Payload ObjectMessage objectMessage, @Header("workerId") String workerId,
            @Header("jobId") String internalJobId) {
        Job job = jobDataService.getById(Long.parseLong(internalJobId));
        // TODO change into Chain of Responsibility type pattern
        Serializable update = null;
        try {
            update = objectMessage.getObject();
        } catch (JMSException e) {
            onJobError(job, e.getMessage());
        }
        if (update instanceof JobEvent) {
            JobEvent jobEvent = (JobEvent) update;
            JobEventType jobEventType = jobEvent.getJobEventType();
            if (jobEventType == JobEventType.DATA_FETCHING_STARTED) {
                onJobDataFetchingStarted(job, workerId);
            } else if (jobEventType == JobEventType.DATA_FETCHING_COMPLETED) {
                onJobDataFetchingCompleted(job);
            } else if (jobEventType == JobEventType.PROCESSING_STARTED) {
                onJobProcessingStarted(job, workerId);
            }
        } else if (update instanceof JobError) {
            JobError jobError = (JobError) update;
            onJobError(job, jobError.getErrorDescription());
        } else if (update instanceof ContainerExit) {
            ContainerExit containerExit = (ContainerExit) update;
            try {
                onContainerExit(job, workerId, containerExit.getJobEnvironment(),
                        containerExit.getExitCode());
            } catch (Exception e) {
                onJobError(job, e.getMessage());
            }
        }
    }

    private void onJobDataFetchingStarted(Job job, String workerId) {
        LOG.info("Downloading input data for {}", job.getExtId());
        job.setWorkerId(workerId);
        job.setStartTime(LocalDateTime.now());
        job.setStatus(Job.Status.RUNNING);
        job.setStage(JobStep.DATA_FETCH.getText());
        jobDataService.save(job);

    }

    private void onJobDataFetchingCompleted(Job job) {
        LOG.info("Launching docker container for job {}", job.getExtId());
    }

    private void onJobProcessingStarted(Job job, String workerId) {
        FstepWorkerBlockingStub worker = workerFactory.getWorkerById(workerId);
        FstepService service = job.getConfig().getService();
        LOG.info("Job {} ({}) launched for service: {}", job.getId(), job.getExtId(),
                service.getName());
        // Update GUI endpoint URL for client access
        if (service.getType() == FstepService.Type.APPLICATION) {
            String zooId = job.getExtId();
            String guiUrl = guiService.getGuiUrl(worker, GrpcUtil.toRpcJob(job));
            LOG.info("Updating GUI URL for job {} ({}): {}", zooId,
                    job.getConfig().getService().getName(), guiUrl);
            job.setGuiUrl(guiUrl);
            jobDataService.save(job);
        }
        job.setStage(JobStep.PROCESSING.getText());
        jobDataService.save(job);

    }

    private void onContainerExit(Job job, String workerId, JobEnvironment jobEnvironment,
            int exitCode) throws Exception {
        FstepWorkerBlockingStub worker = workerFactory.getWorkerById(workerId);
        switch (exitCode) {
            case 0:
                // Normal exit
                break;
            case 137:
                LOG.info("Docker container for {} terminated via SIGKILL (exit code 137)",
                        job.getExtId());
                break;
            case 143:
                LOG.info("Docker container for {} terminated via SIGTERM (exit code 143)",
                        job.getExtId());
                break;
            default:
                throw new Exception("Docker container returned with exit code " + exitCode);
        }
        job.setStage(JobStep.OUTPUT_LIST.getText());
        job.setEndTime(LocalDateTime.now()); // End time is when processing ends
        job.setGuiUrl(null); // Any GUI services will no longer be available
        jobDataService.save(job);
        try {
            ingestOutput(job, GrpcUtil.toRpcJob(job), worker, jobEnvironment);
        } catch (IOException e) {
            throw new Exception("Error ingesting output for : " + e.getMessage());
        }
    }

    private void onJobError(Job job, String description) {
        job.setStatus(Job.Status.ERROR);
        job.setEndTime(LocalDateTime.now());
        jobDataService.save(job);
    }

    private void ingestOutput(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob,
            FstepWorkerBlockingStub worker, JobEnvironment jobEnvironment)
            throws IOException, Exception {
        // Enumerate files in the job output directory
        Map<String, String> outputsByRelativePath =
                listOutputFiles(job, rpcJob, worker, jobEnvironment);
        // Repatriate output files
        Map<String, FstepFile> outputFiles = repatriateAndIngestOutputFiles(job, rpcJob, worker,
                job.getConfig().getInputs(), jobEnvironment, outputsByRelativePath);
        job.setStatus(Job.Status.COMPLETED);
        job.setOutputs(outputFiles.entrySet().stream()
                .collect(toMultimap(e -> e.getKey(), e -> e.getValue().getUri().toString(),
                        MultimapBuilder.hashKeys().hashSetValues()::build)));
        job.setOutputFiles(ImmutableSet.copyOf(outputFiles.values()));
        jobDataService.save(job);
        if (job.getConfig().getService().getType() == FstepService.Type.BULK_PROCESSOR) {
            // Auto-publish the output files
            ImmutableSet.copyOf(outputFiles.values())
                    .forEach(f -> securityService.publish(FstepFile.class, f.getId()));
        }
        if (job.getParentJob() != null) {
            Job parentJob = job.getParentJob();
            if (allChildJobCompleted(parentJob)) {
                completeParentJob(parentJob);
            }
         }
        else {
            StreamObserver<FstepServiceResponse> responseObserver;
            List<JobParam> outputs = outputFiles.entrySet().stream().map(e -> JobParam.newBuilder().setParamName(e.getKey()).addParamValue(e.getValue().getUri().toASCIIString()).build()).collect(toList());
            responseObserver = responseObservers.get(job.getExtId());
            if (responseObserver != null) {
                responseObserver.onNext(FstepServiceResponse.newBuilder().setJobOutputs(FstepServiceResponse.JobOutputs.newBuilder().addAllOutputs(outputs).build()) .build()); 
                responseObserver.onCompleted();
            }
        }
     }

    private void completeParentJob(Job parentJob) {
        StreamObserver<FstepServiceResponse> responseObserver;
        //Must collect all child jobs, save for parent and send a response.
        Multimap<String, FstepFile> jobOutputFiles = collectSubJobOutputs(parentJob);
        // Wrap up the parent job
        parentJob.setStatus(Job.Status.COMPLETED);
        parentJob.setStage(JobStep.OUTPUT_LIST.getText());
        parentJob.setEndTime(LocalDateTime.now());
        parentJob.setGuiUrl(null);
        parentJob.setOutputs(jobOutputFiles.entries().stream().collect(toMultimap(
              e -> e.getKey(),
              e -> e.getValue().getUri().toString(),
              MultimapBuilder.hashKeys().hashSetValues()::build)));
        parentJob.setOutputFiles(ImmutableSet.copyOf(jobOutputFiles.values()));
        jobDataService.save(parentJob);
        List<JobParam> outputs = jobOutputFiles.asMap().entrySet().stream()
                .map(e -> JobParam.newBuilder().setParamName(e.getKey())
                .addAllParamValue(
                e.getValue().stream().map(f -> f.getUri().toASCIIString()).collect(toSet()))
                .build())
                .collect(toList());
        responseObserver = responseObservers.get(parentJob.getExtId());
        if (responseObserver != null) {
            responseObserver.onNext(FstepServiceResponse.newBuilder().setJobOutputs(FstepServiceResponse.JobOutputs.newBuilder().addAllOutputs(outputs).build()) .build()); 
            responseObserver.onCompleted();
        }
        //TODO shall an error be sent in case of any subjobs error?
        //LOG.error("Failed to run processor: {}; notifying gRPC client", description);
        //StreamObserver<FstepServiceResponse> responseObserver = responseObservers.get(parentJob.getExtId());
        //if (responseObserver != null) {
        //  responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withDescription(description)));
        //}
    }

    private SetMultimap<String, FstepFile> collectSubJobOutputs(Job parentJob) {
        SetMultimap<String, FstepFile> jobOutputsFiles = MultimapBuilder.hashKeys().hashSetValues().build();
        for (Job subJob: parentJob.getSubJobs()) {
          subJob.getOutputs().forEach((k, v) -> subJob.getOutputFiles().stream().
                  filter(x -> x.getUri().toString().equals(v)).findFirst().ifPresent(match -> jobOutputsFiles.put(k, match)));
          }
        return jobOutputsFiles;
    }

    private boolean allChildJobCompleted(Job parentJob) {
        return !parentJob.getSubJobs().stream().anyMatch(j -> j.getStatus() != Job.Status.COMPLETED && j.getStatus() != Job.Status.ERROR);
    }

    private Map<String, String> listOutputFiles(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob,
            FstepWorkerGrpc.FstepWorkerBlockingStub worker, JobEnvironment jobEnvironment)
            throws Exception {
        FstepService service = job.getConfig().getService();

        OutputFileList outputFileList = worker.listOutputFiles(ListOutputFilesParam.newBuilder()
                .setJob(rpcJob).setOutputsRootPath(jobEnvironment.getOutputDir()).build());
        List<String> relativePaths = outputFileList.getItemsList().stream()
                .map(OutputFileItem::getRelativePath).collect(toList());

        Map<String, String> outputsByRelativePath;

        if (service.getType() == FstepService.Type.APPLICATION) {
            // Collect all files in the output directory with simple index IDs
            outputsByRelativePath = IntStream.range(0, relativePaths.size()).boxed()
                    .collect(toMap(i -> Integer.toString(i + 1), relativePaths::get));
        } else {
            // Ensure we have one file per expected output
            Set<String> expectedServiceOutputIds = service.getServiceDescriptor().getDataOutputs()
                    .stream().map(FstepServiceDescriptor.Parameter::getId).collect(toSet());
            outputsByRelativePath = new HashMap<>(expectedServiceOutputIds.size());

            for (String expectedOutputId : expectedServiceOutputIds) {
                Optional<String> relativePath = relativePaths.stream()
                        .filter(path -> path.startsWith(expectedOutputId + "/"))
                        .reduce((a, b) -> null);
                if (relativePath.isPresent()) {
                    outputsByRelativePath.put(expectedOutputId, relativePath.get());
                } else {
                    throw new Exception(String.format(
                            "Did not find expected single output for '%s' in outputs list: %s",
                            expectedOutputId, relativePaths));
                }
            }
        }

        return outputsByRelativePath;
    }

    private Map<String, FstepFile> repatriateAndIngestOutputFiles(Job job,
            com.cgi.eoss.fstep.rpc.Job rpcJob, FstepWorkerGrpc.FstepWorkerBlockingStub worker,
            Multimap<String, String> inputs, JobEnvironment jobEnvironment,
            Map<String, String> outputsByRelativePath) throws IOException {
        Map<String, FstepFile> outputFiles = new HashMap<>(outputsByRelativePath.size());
        Map<String, GeoServerSpec> geoServerSpecs = getGeoServerSpecs(inputs); 
        
        for (Map.Entry<String, String> output : outputsByRelativePath.entrySet()) {
            String outputId = output.getKey();
            String relativePath = output.getValue();

            Iterator<OutputFileResponse> outputFile = worker.getOutputFile(GetOutputFileParam
                    .newBuilder().setJob(rpcJob).setPath(Paths.get(jobEnvironment.getOutputDir())
                            .resolve(relativePath).toString())
                    .build());

            // First message is the file metadata
            OutputFileResponse.FileMeta fileMeta = outputFile.next().getMeta();
            LOG.info("Collecting output '{}' with filename {} ({} bytes)", outputId,
                    fileMeta.getFilename(), fileMeta.getSize());
               
            OutputProductMetadata.OutputProductMetadataBuilder outputProductMetadataBuilder = OutputProductMetadata.builder()
                    .owner(job.getOwner()).service(job.getConfig().getService())
                    .jobId(job.getExtId()).crs(Iterables.getOnlyElement(inputs.get("crs"), null))
                    .geometry(Iterables.getOnlyElement(inputs.get("aoi"), null));
            
            HashMap<String, Object> properties = new HashMap<>(ImmutableMap.<String, Object>builder()
                    .put("jobId", job.getExtId()).put("intJobId", job.getId())
                    .put("serviceName", job.getConfig().getService().getName())
                    .put("jobOwner", job.getOwner().getName())
                    .put("jobStartTime",
                            job.getStartTime().atOffset(ZoneOffset.UTC).toString())
                    .put("jobEndTime", job.getEndTime().atOffset(ZoneOffset.UTC).toString())
                    .put("filename", fileMeta.getFilename()).build());
            
            GeoServerSpec geoServerSpecForOutput = geoServerSpecs.get(outputId);
            if (geoServerSpecForOutput != null) {
                properties.put("geoServerSpec", geoServerSpecForOutput);
            }
            
            OutputProductMetadata outputProduct = outputProductMetadataBuilder.properties(properties).build();
            
            // TODO Configure whether files need to be transferred via RPC or simply copied
            Path outputPath = catalogueService.provisionNewOutputProduct(outputProduct,
                    fileMeta.getFilename());
            LOG.info("Writing output file for job {}: {}", job.getExtId(), outputPath);
            try (BufferedOutputStream outputStream = new BufferedOutputStream(
                    Files.newOutputStream(outputPath, CREATE, TRUNCATE_EXISTING, WRITE))) {
                outputFile.forEachRemaining(
                        Unchecked.consumer(of -> of.getChunk().getData().writeTo(outputStream)));
            }

            // Try to read CRS/AOI from the file if not set by input parameters - note that
            // CRS/AOI may still be null after this
            outputProduct.setCrs(
                    Optional.ofNullable(outputProduct.getCrs()).orElse(getOutputCrs(outputPath)));
            outputProduct.setGeometry(Optional.ofNullable(outputProduct.getGeometry())
                    .orElse(getOutputGeometry(outputPath)));
            //TODO This triggers geoserver - change
            outputFiles.put(outputId,
                    catalogueService.ingestOutputProduct(outputProduct, outputPath));
        }

        return outputFiles;
    }

    private Map<String, GeoServerSpec> getGeoServerSpecs(Multimap<String, String> inputs) throws JsonParseException, JsonMappingException, IOException {
        String geoServerSpecsStr = Iterables.getOnlyElement(inputs.get("geoServerSpec"), null);
        Map<String, GeoServerSpec> geoServerSpecs = new HashMap<String, GeoServerSpec>();
        if (geoServerSpecsStr != null) {
            ObjectMapper mapper = new ObjectMapper();
                TypeFactory typeFactory = mapper.getTypeFactory();
                MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, GeoServerSpec.class);
                geoServerSpecs.putAll(mapper.readValue(geoServerSpecsStr, mapType));
        }
        return geoServerSpecs;
    }

    private String getOutputCrs(Path outputPath) {
        try {
            return GeoUtil.extractEpsg(outputPath);
        } catch (Exception e) {
            return null;
        }
    }

    private String getOutputGeometry(Path outputPath) {
        try {
            return GeoUtil.geojsonToWkt(GeoUtil.extractBoundingBox(outputPath));
        } catch (Exception e) {
            return null;
        }
    }

    private void chargeUser(User user, Job job) {
        costingService.chargeForJob(user.getWallet(), job);
    }

}
