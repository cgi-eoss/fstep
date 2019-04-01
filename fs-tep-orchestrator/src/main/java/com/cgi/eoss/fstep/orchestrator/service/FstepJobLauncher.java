package com.cgi.eoss.fstep.orchestrator.service;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.CloseableThreadContext;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.logging.Logging;
import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.model.Databasket;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepService.Type;
import com.cgi.eoss.fstep.model.FstepServiceDockerBuildInfo;
import com.cgi.eoss.fstep.model.FstepServiceResources;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.Job.Status;
import com.cgi.eoss.fstep.model.JobProcessing;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.UserMount;
import com.cgi.eoss.fstep.persistence.service.DatabasketDataService;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.persistence.service.JobProcessingDataService;
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
import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.IngestJobOutputsParams;
import com.cgi.eoss.fstep.rpc.IngestJobOutputsResponse;
import com.cgi.eoss.fstep.rpc.JobOutputsResponse;
import com.cgi.eoss.fstep.rpc.JobOutputsResponse.JobOutputs;
import com.cgi.eoss.fstep.rpc.JobParam;
import com.cgi.eoss.fstep.rpc.JobStatus;
import com.cgi.eoss.fstep.rpc.JobStatusResponse;
import com.cgi.eoss.fstep.rpc.ListWorkersParams;
import com.cgi.eoss.fstep.rpc.RelaunchFailedJobParams;
import com.cgi.eoss.fstep.rpc.RelaunchFailedJobResponse;
import com.cgi.eoss.fstep.rpc.StopServiceParams;
import com.cgi.eoss.fstep.rpc.StopServiceResponse;
import com.cgi.eoss.fstep.rpc.WorkersList;
import com.cgi.eoss.fstep.rpc.worker.DockerImageConfig;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc.FstepWorkerBlockingStub;
import com.cgi.eoss.fstep.rpc.worker.JobSpec;
import com.cgi.eoss.fstep.rpc.worker.ResourceRequest;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.google.common.base.Strings;
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
    
    private static final int SINGLE_JOB_PRIORITY = 9;

    private final CachingWorkerFactory workerFactory;
    private final JobDataService jobDataService;
    private final JobProcessingDataService jobProcessingDataService;
    private final DatabasketDataService databasketDataService;
    private final CostingService costingService;
    private final FstepQueueService queueService;
    private final UserMountDataService userMountDataService;
    private final ServiceDataService serviceDataService;
    private final DynamicProxyService dynamicProxyService;
    private final JobValidator jobValidator;
    private final PlatformParameterExtractor platformParameterExtractor;
    FstepJobUpdatesManager fstepJobUpdatesManager;
    
    @Autowired
    public FstepJobLauncher(CachingWorkerFactory workerFactory, JobDataService jobDataService,
    		JobProcessingDataService jobProcessingDataService,
            DatabasketDataService databasketDataService, FstepGuiServiceManager guiService,
            CostingService costingService,
            FstepSecurityService securityService, FstepQueueService queueService, 
            UserMountDataService userMountDataService,
            ServiceDataService serviceDataService,
            DynamicProxyService dynamicProxyService,
            JobValidator jobValidator,
            FstepJobUpdatesManager fstepJobUpdatesManager) {
        this.workerFactory = workerFactory;
        this.jobDataService = jobDataService;
        this.jobProcessingDataService = jobProcessingDataService;
        this.databasketDataService = databasketDataService;
        this.costingService = costingService;
        this.queueService = queueService;
        this.userMountDataService = userMountDataService;
        this.serviceDataService = serviceDataService;
        this.dynamicProxyService = dynamicProxyService;
        this.jobValidator = jobValidator;
        this.platformParameterExtractor = new PlatformParameterExtractor();
        this.fstepJobUpdatesManager = fstepJobUpdatesManager;
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
                job = jobDataService.refreshFull(Long.valueOf(parentId));
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
            	if (!jobValidator.checkInputs(job.getOwner(), rpcInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested inputs",
                                userId);
                    }
                    throw new ServiceExecutionException(
                            "User does not have read access to all requested inputs");
                }
                
                if (!jobValidator.checkAccessToOutputCollection(job)) {
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
                jobValidator.checkCost(job.getOwner(), job.getConfig(), newInputs);

                if (!jobValidator.checkInputList(job.getOwner(), newInputs)) {
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
                	JobProcessing jobProcessing = jobProcessingDataService.buildNew(subJob);
                    chargeUserForProcessing(subJob.getOwner(), jobProcessing);
                    submitJob(subJob, GrpcUtil.toRpcJob(subJob),
                            GrpcUtil.mapToParams(subJob.getConfig().getInputs()), getJobPriority(i));
                    i++;
                }

            } 
            else {
            	if (!Strings.isNullOrEmpty(parentId)) {
            		Job subJob = jobDataService.buildNew(zooId, userId, serviceId, jobConfigLabel, inputs, job);
            		submitSingleJob(userId, rpcInputs, subJob, GrpcUtil.toRpcJob(subJob));
                    
            	}
            	else {
            		submitSingleJob(userId, rpcInputs, job, rpcJob);
            	}
            }
            responseObserver.onCompleted();

        } catch (Exception e) {
            if (job != null) {
                endJobWithError(job);
            }

            LOG.error("Failed to run processor. Notifying gRPC client", e);
            responseObserver.onError(new StatusRuntimeException(
                    io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        }
    }

	private void submitSingleJob(String userId, List<JobParam> rpcInputs, Job job, com.cgi.eoss.fstep.rpc.Job rpcJob)
			throws IOException {
		jobValidator.checkCost(job.getOwner(), job.getConfig());
		if (!jobValidator.checkInputs(job.getOwner(), rpcInputs)) {
		    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
		        LOG.error("User {} does not have read access to all requested inputs",
		                userId);
		    }
		    throw new ServiceExecutionException(
		            "User does not have read access to all requested inputs");
		}
           //TODO: Check that the user can use the geoserver spec
		if (!jobValidator.checkAccessToOutputCollection(job)) {
		    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
		        LOG.error("User {} does not have read access to all requested output collections",
		                userId);
		    }
		    throw new ServiceExecutionException(
		            "User does not have read access to all requested output collections");
		}
        CostQuotation costQuotation = costingService.estimateJobCost(job.getConfig());
        job.setCostQuotation(costQuotation);
        jobDataService.save(job);
        JobProcessing jobProcessing = jobProcessingDataService.buildNew(job);
        chargeUserForProcessing(job.getOwner(), jobProcessing);
		submitJob(job, rpcJob, rpcInputs, SINGLE_JOB_PRIORITY);
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
	    Job job = jobDataService.refreshFull(Long.parseLong(rpcJob.getIntJobId()));
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
	public void ingestJobOutputs(IngestJobOutputsParams request,
	        StreamObserver<IngestJobOutputsResponse> responseObserver) {
	    com.cgi.eoss.fstep.rpc.Job rpcJob = request.getJob();
	    Job job = jobDataService.refreshFull(Long.parseLong(rpcJob.getIntJobId()));
	    Set<Job> subJobs = new HashSet<>(job.getSubJobs());
	    try {
		    if (subJobs.size() > 0) {
		        for (Job subJob : subJobs) {
		            if (subJob.getStatus().equals(Status.ERROR) && "Step 3 of 3: Output-List".equals(subJob.getStage())){
		            	subJob = jobDataService.refreshFull(subJob);
		            	ingestJobOutputs(subJob);
		            }
		        }
		    } else {
		    	if (job.getStatus().equals(Status.ERROR) && "Step 3 of 3: Output-List".equals(job.getStage())) {
		    		ingestJobOutputs(job);
		        }
		    }
		    responseObserver.onNext(IngestJobOutputsResponse.newBuilder().build());
		    responseObserver.onCompleted();
	    }
	    catch (Exception e) {
	    	LOG.error(e);
	    	responseObserver.onError(e);
	    }
	}

	@Override
    public void relaunchFailedJob(RelaunchFailedJobParams request, StreamObserver<RelaunchFailedJobResponse> responseObserver) {

        com.cgi.eoss.fstep.rpc.Job rpcJob = request.getJob();
        Job job = jobDataService.refreshFull(Long.parseLong(rpcJob.getIntJobId()));
        responseObserver.onNext(RelaunchFailedJobResponse.newBuilder().build());
        try (CloseableThreadContext.Instance ctc =
                CloseableThreadContext.push("FS-TEP Service Orchestrator").put("userId", String.valueOf(job.getOwner().getId()))
                        .put("serviceId", String.valueOf(job.getConfig().getService().getId())).put("zooId", job.getExtId())) {
            if (job.getConfig().getService().getType() == Type.PARALLEL_PROCESSOR && job.isParent()) {
                Set<Job> failedSubJobs =
                        job.getSubJobs().stream().filter(j -> j.getStatus() == Status.ERROR).collect(Collectors.toSet());
                if (failedSubJobs.size() > 0) {
                	jobValidator.checkCost(job.getOwner(), failedSubJobs);
                    //TODO: Check that the user can use the geoserver spec
                    for (Job failedSubJob : failedSubJobs) {
                        List<JobParam> failedSubJobInputs = GrpcUtil.mapToParams(failedSubJob.getConfig().getInputs());
                        if (!jobValidator.checkInputs(job.getOwner(), failedSubJobInputs)) {
                            try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                                LOG.error("User {} does not have read access to all requested inputs", job.getOwner().getId());
                            }
                            throw new ServiceExecutionException("User does not have read access to all requested inputs");
                        }

                        if (!jobValidator.checkAccessToOutputCollection(job)) {
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
                        JobProcessing jobProcessing = jobProcessingDataService.buildNew(failedSubJob);
                        if (failedSubJob.getStage().equals("Step 1 of 3: Data-Fetch") == false) {
                        	chargeUserForProcessing(failedSubJob.getOwner(), jobProcessing);
                        }
                        failedSubJob.setStatus(Status.CREATED);
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
                jobValidator.checkCost(job.getOwner(), job);
                //TODO: Check that the user can use the geoserver spec
                if (!jobValidator.checkInputs(job.getOwner(), jobInputs)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested inputs", job.getOwner().getId());
                    }
                    throw new ServiceExecutionException("User does not have read access to all requested inputs");
                }

                if (!jobValidator.checkAccessToOutputCollection(job)) {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.error("User {} does not have read access to all requested output collections", job.getOwner().getId());
                    }
                    throw new ServiceExecutionException("User does not have read access to all requested output collections");
                }
                JobProcessing jobProcessing = jobProcessingDataService.buildNew(job);
                if (job.getStage() != null && !job.getStage().equals("Step 1 of 3: Data-Fetch")) {
                	chargeUserForProcessing(job.getOwner(), jobProcessing);
                }
                job.setStatus(Status.CREATED);
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
    

    @Override
    public void getJobStatus(com.cgi.eoss.fstep.rpc.GetJobStatusParams request,
        io.grpc.stub.StreamObserver<com.cgi.eoss.fstep.rpc.JobStatusResponse> responseObserver) {
    	com.cgi.eoss.fstep.rpc.Job rpcJob = request.getJob();
    	Job job = jobDataService.getById(Long.parseLong(rpcJob.getIntJobId()));
    	JobStatusResponse r = JobStatusResponse.newBuilder()
    			.setJobStatus(JobStatus.newBuilder().setStatus(JobStatus.Status.valueOf(job.getStatus().toString()))).build();
    	responseObserver.onNext(r);
    	responseObserver.onCompleted();
    }

    @Override
    public void getJobOutputs(com.cgi.eoss.fstep.rpc.GetJobOutputsParams request,
        io.grpc.stub.StreamObserver<com.cgi.eoss.fstep.rpc.JobOutputsResponse> responseObserver) {
    	com.cgi.eoss.fstep.rpc.Job rpcJob = request.getJob();
    	Job job = jobDataService.getById(Long.parseLong(rpcJob.getIntJobId()));
    	Multimap<String, String> jobOutputs = job.getOutputs();
    	JobOutputsResponse r = JobOutputsResponse.newBuilder()
    			.setJobOutputs(JobOutputs.newBuilder().addAllOutputs(GrpcUtil.mapToParams(jobOutputs))).build();
    	responseObserver.onNext(r);
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

    private void submitJob(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob, List<JobParam> rpcInputs, int priority)
            throws IOException {
        FstepService service = job.getConfig().getService();
        JobSpec.Builder jobSpecBuilder = JobSpec.newBuilder()
                .setService(GrpcUtil.toRpcService(service)).setJob(rpcJob).addAllInputs(rpcInputs);
        if (service.getType() == FstepService.Type.APPLICATION) {
            jobSpecBuilder.addExposedPorts(FstepGuiServiceManager.GUACAMOLE_PORT);
        }
        Integer timeout = platformParameterExtractor.getTimeout(job);
        jobSpecBuilder = jobSpecBuilder.setTimeoutValue(timeout);
        
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
        queueService.sendObject(FstepQueueService.jobPendingQueueName, getJobHeaders(job), jobSpec, priority);
        job.setStatus(Status.PENDING);
        jobDataService.save(job);
    }

	private HashMap<String, Object> getJobHeaders(Job job) {
		HashMap<String, Object> messageHeaders = new HashMap<String, Object>();
        messageHeaders.put("jobId", String.valueOf(job.getId()));
		return messageHeaders;
	}

    private void cancelJob(Job job) {
        LOG.info("Cancelling job with id {}", job.getId());
        JobSpec queuedJobSpec = null;
        if (job.getStatus().equals(Status.PENDING)) {
        	queuedJobSpec = (JobSpec) queueService
                .receiveSelectedObjectNoWait(FstepQueueService.jobPendingQueueName, "jobId = '" + job.getId() + "'");
        }
        else if (job.getStatus().equals(Status.WAITING)) {
        	queuedJobSpec = (JobSpec) queueService
                    .receiveSelectedObjectNoWait(FstepQueueService.jobExecutionQueueName, "jobId = '" + job.getId() +"'");
        }
        
        if (queuedJobSpec != null) {
            LOG.info("Refunding user for job id {}", job.getId());
            JobProcessing jobProcessing = jobProcessingDataService.findByJobAndMaxSequenceNum(job);
            if (jobProcessing != null) {
            	costingService.refundJobProcessing(job.getOwner().getWallet(), jobProcessing);
            }
            job.setStatus(Status.CANCELLED);
            jobDataService.save(job);
        }
    }
    
    private void ingestJobOutputs(Job job) throws IOException, Exception {
    	FstepWorkerBlockingStub worker = workerFactory.getWorkerById(job.getWorkerId());
    	//TODO The output location is hardwired, but there is no way for the server to know it in this retry call because
    	//it is normally transmitted to the server by the worker in the containerExit rpc call
    	//Either the location should be saved together with the job status or the outputLocation call should be made available by the worker
    	String outputRootPath = "/data/jobs/Job_" + job.getExtId() + "/outDir";
        fstepJobUpdatesManager.ingestOutput(job, GrpcUtil.toRpcJob(job), worker, outputRootPath);
        
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
            CostQuotation costQuotation = costingService.estimateSingleRunJobCost(childJob.getConfig());
            childJob.setCostQuotation(costQuotation);
            childJob = jobDataService.save(childJob);
            parentJob.getSubJobs().add(childJob);
            childJobs.add(childJob);
        }
        parentJob.setParent(true);
        jobDataService.save(parentJob);
        return childJobs;
    }
    

    private void chargeUserForProcessing(User user, JobProcessing jobProcessing) {
        costingService.chargeForJobProcessing(user.getWallet(), jobProcessing);
    }
 
    @Override
    public void listWorkers(ListWorkersParams request, StreamObserver<WorkersList> responseObserver) {
        try {
            responseObserver.onNext(workerFactory.listWorkers());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to enumerate workers", e);
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        }
    }

}
