package com.cgi.eoss.fstep.orchestrator.service;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.logging.Logging;
import com.cgi.eoss.fstep.model.Databasket;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepService.Type;
import com.cgi.eoss.fstep.model.FstepServiceDockerBuildInfo;
import com.cgi.eoss.fstep.model.FstepServiceResources;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.Job.Status;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.UserMount;
import com.cgi.eoss.fstep.persistence.service.DatabasketDataService;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
import com.cgi.eoss.fstep.persistence.service.UserMountDataService;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.rpc.BuildServiceParams;
import com.cgi.eoss.fstep.rpc.BuildServiceResponse;
import com.cgi.eoss.fstep.rpc.CancelJobParams;
import com.cgi.eoss.fstep.rpc.CancelJobResponse;
import com.cgi.eoss.fstep.rpc.FstepJobLauncherGrpc;
import com.cgi.eoss.fstep.rpc.FstepJobResponse;
import com.cgi.eoss.fstep.rpc.FstepServiceParams;
import com.cgi.eoss.fstep.rpc.FstepServiceResponse;
import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.JobParam;
import com.cgi.eoss.fstep.rpc.RelaunchFailedJobParams;
import com.cgi.eoss.fstep.rpc.RelaunchFailedJobResponse;
import com.cgi.eoss.fstep.rpc.StopServiceParams;
import com.cgi.eoss.fstep.rpc.StopServiceResponse;
import com.cgi.eoss.fstep.rpc.worker.DockerImageConfig;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc.FstepWorkerBlockingStub;
import com.cgi.eoss.fstep.rpc.worker.JobSpec;
import com.cgi.eoss.fstep.rpc.worker.ResourceRequest;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;
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

    private final CachingWorkerFactory workerFactory;
    private final JobDataService jobDataService;
    private final DatabasketDataService databasketDataService;
    private final CatalogueService catalogueService;
    private final CostingService costingService;
    private final FstepQueueService queueService;
    private final UserMountDataService userMountDataService;
    private final ServiceDataService serviceDataService;
    private final DynamicProxyService dynamicProxyService;
    @Value("${fstep.orchestrator.gui.baseUrl:http://fstep}")
    private String baseUrl;
    
    @Autowired
    public FstepJobLauncher(CachingWorkerFactory workerFactory, JobDataService jobDataService,
            DatabasketDataService databasketDataService, FstepGuiServiceManager guiService,
            CatalogueService catalogueService, CostingService costingService,
            FstepSecurityService securityService, FstepQueueService queueService, 
            UserMountDataService userMountDataService,
            ServiceDataService serviceDataService,
            DynamicProxyService dynamicProxyService) {
        this.workerFactory = workerFactory;
        this.jobDataService = jobDataService;
        this.databasketDataService = databasketDataService;
        this.catalogueService = catalogueService;
        this.costingService = costingService;
        this.queueService = queueService;
        this.userMountDataService = userMountDataService;
        this.serviceDataService = serviceDataService;
        this.dynamicProxyService = dynamicProxyService;
    }

    @Override
    public void submitJob(FstepServiceParams request,
            StreamObserver<FstepJobResponse> responseObserver) {
        String zooId = request.getJobId();
        String userId = request.getUserId();
        String serviceId = request.getServiceId();
        String parentId = request.getJobParent();
        String jobConfigLabel = request.getJobConfigLabel();
        List<JobParam> rpcInputs = request.getInputsList();
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);

        Job job = null;
        com.cgi.eoss.fstep.rpc.Job rpcJob = null;
        try (CloseableThreadContext.Instance ctc =
                CloseableThreadContext.push("FS-TEP Service Orchestrator").put("userId", userId)
                        .put("serviceId", serviceId).put("zooId", zooId)) {
            
            if (!Strings.isNullOrEmpty(parentId)) {
                //this is a request to attach a subjob to an existing parent
                job = jobDataService.reload(Long.valueOf(parentId));
                FstepService service = job.getConfig().getService();
                if (service.getType() != FstepService.Type.PARALLEL_PROCESSOR) {
                    throw new ServiceExecutionException(
                            "Trying to attach a new subjob to a non parallel job");
                }
            }
           
            else {
                job = jobDataService.buildNew(zooId, userId, serviceId, jobConfigLabel, inputs);
            } 

            rpcJob = GrpcUtil.toRpcJob(job);
            // Post back the job metadata for async responses
            responseObserver.onNext(FstepJobResponse.newBuilder().setJob(rpcJob).build());

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
                
                if (!checkAccessToOutputCollection(job.getOwner(), rpcInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested output collections",
                                userId);
                    }
                    throw new ServiceExecutionException(
                            "User does not have read access to all requested output collections");
                }
                
                //TODO: Check that the user can use the geoserver spec

                Collection<String> parallelInput = inputs.get("parallelInputs");
                List<String> newInputs = explodeParallelInput(parallelInput);
                checkCost(job.getOwner(), job.getConfig(), newInputs);

                if (!checkInputList(job.getOwner(), newInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested inputs",
                                userId);
                    }
                    throw new ServiceExecutionException(
                            "User does not have read access to all requested inputs");
                }
                jobDataService.save(job);
                List<Job> subJobs = createSubJobs(job, userId, service, newInputs, inputs);

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
                if (!checkInputs(job.getOwner(), rpcInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested inputs",
                                userId);
                    }
                    throw new ServiceExecutionException(
                            "User does not have read access to all requested inputs");
                }
              //TODO: Check that the user can use the geoserver spec
                if (!checkAccessToOutputCollection(job.getOwner(), rpcInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested output collections",
                                userId);
                    }
                    throw new ServiceExecutionException(
                            "User does not have read access to all requested output collections");
                }
                
                chargeUser(job.getOwner(), job);
                submitJob(job, rpcJob, rpcInputs, SINGLE_JOB_PRIORITY);
            }

        } catch (Exception e) {
            if (job != null) {
                endJobWithError(job);
            }

            LOG.error("Failed to run processor. Notifying gRPC client", e);
            responseObserver.onError(new StatusRuntimeException(
                    io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
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
	        worker.stopContainer(rpcJob);
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

	@Override
    public void relaunchFailedJob(RelaunchFailedJobParams request, StreamObserver<RelaunchFailedJobResponse> responseObserver) {

        com.cgi.eoss.fstep.rpc.Job rpcJob = request.getJob();
        Job job = jobDataService.reload(Long.parseLong(rpcJob.getIntJobId()));
        responseObserver.onNext(RelaunchFailedJobResponse.newBuilder().build());
        try (CloseableThreadContext.Instance ctc =
                CloseableThreadContext.push("FS-TEP Service Orchestrator").put("userId", String.valueOf(job.getOwner().getId()))
                        .put("serviceId", String.valueOf(job.getConfig().getService().getId())).put("zooId", job.getExtId())) {
            if (job.getConfig().getService().getType() == Type.PARALLEL_PROCESSOR && job.isParent()) {
                Set<Job> failedSubJobs =
                        job.getSubJobs().stream().filter(j -> j.getStatus() == Status.ERROR).collect(Collectors.toSet());
                if (failedSubJobs.size() > 0) {
                    checkCost(job.getOwner(), job.getConfig(), failedSubJobs.size());
                    //TODO: Check that the user can use the geoserver spec
                    for (Job failedSubJob : failedSubJobs) {
                        List<JobParam> failedSubJobInputs = GrpcUtil.mapToParams(failedSubJob.getConfig().getInputs());
                        if (!checkInputs(job.getOwner(), failedSubJobInputs)) {
                            try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                                LOG.error("User {} does not have read access to all requested inputs", job.getOwner().getId());
                            }
                            throw new ServiceExecutionException("User does not have read access to all requested inputs");
                        }

                        if (!checkAccessToOutputCollection(job.getOwner(), failedSubJobInputs)) {
                            try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                                LOG.error("User {} does not have read access to all requested output collections",
                                        job.getOwner().getId());
                            }
                            throw new ServiceExecutionException("User does not have read access to all requested output collections");
                        }
                    }
                    int i = 0;
                    for (Job failedSubJob : failedSubJobs) {
                        List<JobParam> failedSubJobInputs = GrpcUtil.mapToParams(failedSubJob.getConfig().getInputs());
                        if (failedSubJob.getStage().equals("Step 1 of 3: Data-Fetch") == false) {
                        	chargeUser(failedSubJob.getOwner(), failedSubJob);
                        }
                        failedSubJob.setStatus(Status.CREATED);
                        failedSubJob.setStartTime(null);
                        failedSubJob.setEndTime(null);
                        failedSubJob.setStage(null);
                        failedSubJob.setWorkerId(null);
                        jobDataService.save(failedSubJob);
                        submitJob(failedSubJob, GrpcUtil.toRpcJob(failedSubJob), failedSubJobInputs, getJobPriority(i));
                    }
                    i++;
                }
            } else {
                List<JobParam> jobInputs = GrpcUtil.mapToParams(job.getConfig().getInputs());
                checkCost(job.getOwner(), job.getConfig());
                //TODO: Check that the user can use the geoserver spec
                if (!checkInputs(job.getOwner(), jobInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested inputs", job.getOwner().getId());
                    }
                    throw new ServiceExecutionException("User does not have read access to all requested inputs");
                }

                if (!checkAccessToOutputCollection(job.getOwner(), jobInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested output collections", job.getOwner().getId());
                    }
                    throw new ServiceExecutionException("User does not have read access to all requested output collections");
                }
                if (job.getStage() != null && !job.getStage().equals("Step 1 of 3: Data-Fetch")) {
                	chargeUser(job.getOwner(), job);
                }
                job.setStatus(Status.CREATED);
                job.setStartTime(null);
                job.setEndTime(null);
                job.setStage(null);
                job.setWorkerId(null);
                jobDataService.save(job);
                submitJob(job, rpcJob, GrpcUtil.mapToParams(job.getConfig().getInputs()), SINGLE_JOB_PRIORITY);
            }
        } catch (Exception e) {
            if (job != null) {
                endJobWithError(job);
            }

            LOG.error("Failed to run processor. Notifying gRPC client", e);
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        }
    }
    
    @Override
	public void buildService(BuildServiceParams buildServiceParams,
	        StreamObserver<BuildServiceResponse> responseObserver) {
    	Long serviceId = Long.parseLong(buildServiceParams.getServiceId());
    	FstepService service = serviceDataService.getById(serviceId);
	    try {
	        FstepWorkerBlockingStub worker = workerFactory.getOne();
		    responseObserver.onNext(BuildServiceResponse.newBuilder().build());
		    DockerImageConfig dockerImageConfig = DockerImageConfig.newBuilder()
		    .setDockerImage(service.getDockerTag())
		    .setServiceName(service.getName())
		    .build();
		    worker.prepareDockerImage(dockerImageConfig);
	        service.getDockerBuildInfo().setDockerBuildStatus(FstepServiceDockerBuildInfo.Status.COMPLETED);
	        service.getDockerBuildInfo().setLastBuiltFingerprint(buildServiceParams.getBuildFingerprint());
	        serviceDataService.save(service);
	    }
	    catch(Exception e){
	        service.getDockerBuildInfo().setDockerBuildStatus(FstepServiceDockerBuildInfo.Status.ERROR);
	        serviceDataService.save(service);
	    }
	    responseObserver.onCompleted();
	}

	private void endJobWithError(Job job) {
        job.setStatus(Job.Status.ERROR);
        job.setEndTime(LocalDateTime.now());
        jobDataService.save(job);
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
    
    private void checkCost(User user, JobConfig config, List<String> newInputs) {
        int singleJobCost = costingService.estimateSingleRunJobCost(config);
        int estimatedCost = newInputs.size() * singleJobCost;
        if (estimatedCost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException(
                    "Estimated cost (" + estimatedCost + " coins) exceeds current wallet balance");
        }
    }
    
    private void checkCost(User user, JobConfig config, int numberOfJobs) {
        int singleJobCost = costingService.estimateSingleRunJobCost(config);
        int estimatedCost = numberOfJobs * singleJobCost;
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
        
    private boolean checkAccessToOutputCollection(User user, List<JobParam> rpcInputs) {
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);
        Map<String, String> collectionSpecs;
        try {
            collectionSpecs = getCollectionSpecs(inputs);
            return collectionSpecs.values().stream()
                    .allMatch(collectionId -> catalogueService.canUserWrite(user, collectionId));
        } catch (IOException e) {
            return false;
        }
    }
    
    private Map<String, String> getCollectionSpecs(Multimap<String, String> inputs) throws JsonParseException, JsonMappingException, IOException {
        String collectionsStr = Iterables.getOnlyElement(inputs.get("collection"), null);
        Map<String, String> collectionSpecs = new HashMap<String, String>();
        if (collectionsStr != null && collectionsStr.length() > 0) {
            ObjectMapper mapper = new ObjectMapper();
                TypeFactory typeFactory = mapper.getTypeFactory();
                MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, String.class);
                collectionSpecs.putAll(mapper.readValue(collectionsStr, mapType));
        }
        return collectionSpecs;
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
        
        Map<Long, String> additionalMounts = job.getConfig().getService().getAdditionalMounts();
        
        for (Long userMountId : additionalMounts.keySet()) {
            UserMount userMount = userMountDataService.getById(userMountId);
            String targetPath = additionalMounts.get(userMountId);
            String bind = userMount.getMountPath() + ":" + targetPath + ":" + userMount.getType().toString().toLowerCase();
            jobSpecBuilder.addUserBinds(bind);
        }
        
        if (service.getType().equals(Type.APPLICATION) && dynamicProxyService.supportsProxyRoute()) {
        	jobSpecBuilder.putEnvironmentVariables("PLATFORM_REVERSE_PROXY_PREFIX", dynamicProxyService.getProxyRoute(rpcJob));
        }        
        //TODO Add CPU, RAM Management
        //TODO Add per job requests
        if (service.getRequiredResources() != null) {
            FstepServiceResources requiredResources = service.getRequiredResources();
            jobSpecBuilder.setResourceRequest(ResourceRequest.newBuilder().setStorage(Integer.valueOf(requiredResources.getStorage())));
        }
        
        JobSpec jobSpec = jobSpecBuilder.build();
        HashMap<String, Object> messageHeaders = new HashMap<String, Object>();
        messageHeaders.put("jobId", job.getId());
        queueService.sendObject(FstepQueueService.jobQueueName, messageHeaders, jobSpec, priority);
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
            childJob = jobDataService.reload(childJob.getId());
            parentJob.getSubJobs().add(childJob);
            childJobs.add(childJob);
        }
        parentJob.setParent(true);
        jobDataService.save(parentJob);
        return childJobs;
    }
    

    private void chargeUser(User user, Job job) {
        costingService.chargeForJob(user.getWallet(), job);
    }
 

}
