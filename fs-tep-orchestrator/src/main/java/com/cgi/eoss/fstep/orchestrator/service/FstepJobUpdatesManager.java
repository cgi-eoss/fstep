package com.cgi.eoss.fstep.orchestrator.service;

import static com.google.common.collect.Multimaps.toMultimap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.CloseableThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.logging.Logging;
import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.model.CostQuotation.Recurrence;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobProcessing;
import com.cgi.eoss.fstep.model.JobStep;
import com.cgi.eoss.fstep.model.Wallet;
import com.cgi.eoss.fstep.model.WalletTransaction;
import com.cgi.eoss.fstep.orchestrator.utils.ModelToGrpcUtils;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.persistence.service.JobProcessingDataService;
import com.cgi.eoss.fstep.persistence.service.WalletDataService;
import com.cgi.eoss.fstep.persistence.service.WalletTransactionDataService;
import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc.FstepWorkerBlockingStub;
import com.cgi.eoss.fstep.rpc.worker.GetOutputFileParam;
import com.cgi.eoss.fstep.rpc.worker.ListOutputFilesParam;
import com.cgi.eoss.fstep.rpc.worker.OutputFileItem;
import com.cgi.eoss.fstep.rpc.worker.OutputFileList;
import com.cgi.eoss.fstep.rpc.worker.PortBinding;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.protobuf.Timestamp;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class FstepJobUpdatesManager {
	
	private final JobDataService jobDataService;
    private final DynamicProxyService dynamicProxyService;
    private final FstepGuiServiceManager guiService;
    private final CachingWorkerFactory workerFactory;
    private final FstepSecurityService securityService;
    private final JobProcessingDataService jobProcessingDataService;
    private final CostingService costingService;
    private final WalletDataService walletDataService;
    private final WalletTransactionDataService walletTransactionDataService;
	private final OutputIngestionService outputIngestionService;
	
	 @Autowired
	    public FstepJobUpdatesManager(JobDataService jobDataService, 
	    		DynamicProxyService dynamicProxyService, 
	    		FstepGuiServiceManager guiService, 
	    		CachingWorkerFactory workerFactory,
	    		FstepSecurityService securityService,
	    		JobProcessingDataService jobProcessingDataService,
	    		CostingService costingService, 
	    		WalletDataService walletDataService,
	    		WalletTransactionDataService walletTransactionDataService,
	    		OutputIngestionService outputIngestionService) {
	        this.jobDataService = jobDataService;
	        this.dynamicProxyService = dynamicProxyService;
	        this.guiService = guiService;
	        this.workerFactory = workerFactory;
	        this.securityService = securityService;
	        this.jobProcessingDataService = jobProcessingDataService;
	        this.costingService = costingService;
	        this.walletDataService = walletDataService;
	        this.walletTransactionDataService = walletTransactionDataService;
	        this.outputIngestionService = outputIngestionService;
	    }

    void onJobHeartbeat(Job job, Timestamp timestamp) {
    	LOG.debug("Received heartbeat for job {}", job.getId());
    	JobProcessing jobProcessing = jobProcessingDataService.findByJobAndMaxSequenceNum(job);
    	if (jobProcessing == null) {
    		return;
    	}
    	OffsetDateTime newHeartbeat = GrpcUtil.offsetDateTimeFromTimestamp(timestamp);
		CostQuotation costQuotation = job.getCostQuotation();
		if (costQuotation == null) {
			costQuotation = costingService.estimateSingleRunJobCost(job.getConfig());
		}
		if (costQuotation.getRecurrence().equals(Recurrence.HOURLY)) {
			OffsetDateTime lastHeartbeat = jobProcessing.getLastHeartbeat();
			Duration lastHeartbeatDifference = Duration.between(jobProcessing.getStartProcessingTime(), lastHeartbeat);
			Duration newHeartbeatDifference = Duration.between(jobProcessing.getStartProcessingTime(), newHeartbeat);
			long hoursWithLastHeartbeat = lastHeartbeatDifference.toHours();
			long hoursWithNewHeartbeat = newHeartbeatDifference.toHours();
			if (hoursWithNewHeartbeat > hoursWithLastHeartbeat) {
				int hoursToCharge = (int) (hoursWithNewHeartbeat - hoursWithLastHeartbeat);
				int amountToCharge = hoursToCharge * costQuotation.getCost();
				Wallet wallet = walletDataService.findByOwner(job.getOwner());
				if (wallet.getBalance() > amountToCharge) {
					costingService.transactForJobProcessing(wallet, jobProcessing, amountToCharge * -1);
				}
				else {
					//Stop processing due to insufficient credits
					FstepWorkerGrpc.FstepWorkerBlockingStub worker =
			                workerFactory.getWorkerById(job.getWorkerId());
					worker.stopContainer(ModelToGrpcUtils.toRpcJob(job));
				}
			}
		}
    	jobProcessing.setLastHeartbeat(newHeartbeat);
    	jobProcessingDataService.save(jobProcessing);
    }

	void onJobDataFetchingStarted(Job job, String workerId) {
        LOG.info("Downloading input data for {}", job.getExtId());
        job.setWorkerId(workerId);
        //Update the start time if this is the first job execution
        if (job.getStartTime() == null) {
        	job.setStartTime(LocalDateTime.now());
        }
        job.setStatus(Job.Status.RUNNING);
        job.setStage(JobStep.DATA_FETCH.getText());
        jobDataService.save(job);

    }

    void onJobDataFetchingCompleted(Job job) {
        LOG.info("Launching docker container for job {}", job.getExtId());
    }

    void onJobProcessingStarted(Job job, String workerId, Timestamp timestamp) {
        FstepService service = job.getConfig().getService();
        LOG.info("Job {} ({}) launched for service: {}", job.getId(), job.getExtId(),
                service.getName());
        // Update GUI endpoint URL for client access
        if (service.getType() == FstepService.Type.APPLICATION) {
            String zooId = job.getExtId();
            FstepWorkerBlockingStub worker = workerFactory.getWorkerById(workerId);
            com.cgi.eoss.fstep.rpc.Job rpcJob = ModelToGrpcUtils.toRpcJob(job);
            PortBinding portBinding = guiService.getGuiPortBinding(worker, rpcJob);
            ReverseProxyEntry guiEntry = dynamicProxyService.getProxyEntry(rpcJob, portBinding.getBinding().getIp(), portBinding.getBinding().getPort());
            LOG.info("Updating GUI URL for job {} ({}): {}", zooId,
                    job.getConfig().getService().getName(), guiEntry.getPath());
            job.setGuiUrl(guiEntry.getPath());
            job.setGuiEndpoint(guiEntry.getEndpoint());
            jobDataService.save(job);
            dynamicProxyService.update();
        }
        JobProcessing jobProcessing = jobProcessingDataService.findByJobAndMaxSequenceNum(job);
        if (jobProcessing != null) {
	        OffsetDateTime startProcessingTime = GrpcUtil.offsetDateTimeFromTimestamp(timestamp);
			jobProcessing.setStartProcessingTime(startProcessingTime);
	        jobProcessing.setLastHeartbeat(startProcessingTime);
	        jobProcessingDataService.save(jobProcessing);
        }
        job.setStage(JobStep.PROCESSING.getText());
        jobDataService.save(job);

    }

    void onContainerExit(Job job, String workerId, String outputRootPath,
            int exitCode, Timestamp timestamp) throws Exception {
        JobProcessing jobProcessing = jobProcessingDataService.findByJobAndMaxSequenceNum(job);
        if (jobProcessing != null) {
	        jobProcessing.setEndProcessingTime(GrpcUtil.offsetDateTimeFromTimestamp(timestamp));
	        jobProcessingDataService.save(jobProcessing);
        }
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
        reconcileCost(job);
        job.setGuiUrl(null); // Any GUI services will no longer be available
        job.setGuiEndpoint(null); // Any GUI services will no longer be available
        jobDataService.save(job);
        try {
        	FstepWorkerBlockingStub worker = workerFactory.getWorkerById(workerId);
            ingestOutput(job, ModelToGrpcUtils.toRpcJob(job), worker, outputRootPath);
        } catch (IOException e) {
            throw new Exception("Error ingesting output for : " + e.getMessage());
        }
    }

    private void reconcileCost(Job job) {
    	CostQuotation costQuotation = job.getCostQuotation();
		if (costQuotation == null) {
			costQuotation = costingService.estimateSingleRunJobCost(job.getConfig());
		}
		if (costQuotation.getRecurrence().equals(Recurrence.HOURLY)) {
			//Find the latest job processing for this job
	    	JobProcessing jobProcessing = jobProcessingDataService.findByJobAndMaxSequenceNum(job);
	    	if (jobProcessing == null) {
	    		return;
	    	}
			//check how many coins have been charged for this job processing
	    	int amountCharged = walletTransactionDataService.findByTypeAndAssociatedId(WalletTransaction.Type.JOB_PROCESSING, jobProcessing.getId()).stream().mapToInt(t -> t.getBalanceChange() * -1).sum();
	    	//check how many coins should be charged for this job processing
	    	Duration totalRuntime = Duration.between(jobProcessing.getStartProcessingTime(), jobProcessing.getEndProcessingTime());
	    	long runtimeSeconds = totalRuntime.getSeconds();
			int numHours = (int) (runtimeSeconds/3600);
	    	if (runtimeSeconds % 3600  != 0) {
	    		numHours++;
	    	}
	    	int amountChargeable = costQuotation.getCost() * numHours;
	    	//Transaction amount is positive if we charged more than we should have
			int transactionAmount = amountCharged - amountChargeable;
			if (transactionAmount != 0) {
				costingService.transactForJobProcessing(job.getOwner().getWallet(), jobProcessing, transactionAmount);
			}
		}
	}

	void onJobError(Job job, String description) {
        LOG.error("Error in Job {}: {}",
                job.getExtId(), description);
        endJobWithError(job);
    }

    void onJobError(Job job, Throwable t) {
        LOG.error("Error in Job " + job.getExtId(), t);
        endJobWithError(job);
    }
    
    private void endJobWithError(Job job) {
        job.setStatus(Job.Status.ERROR);
        job.setEndTime(LocalDateTime.now());
        jobDataService.save(job);
        if (job.getParentJob() != null) {
        	Job parentJob = job.getParentJob();
			parentJob .setStatus(Job.Status.ERROR);
			jobDataService.save(parentJob);
        }
    }
    
    public void ingestOutput(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob,
            FstepWorkerBlockingStub worker, String outputRootPath) throws IOException, InterruptedException {
        // Enumerate files in the job output directory
        Multimap<String, String> outputsByRelativePath =
                listOutputFiles(job, rpcJob, worker, outputRootPath);
        // Repatriate output files
        Multimap<String, FstepFile> outputFiles = repatriateAndIngestOutputFiles(job, rpcJob, worker, outputRootPath, outputsByRelativePath);
        job.setStatus(Job.Status.COMPLETED);
        job.setOutputs(outputFiles.entries().stream()
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
        	Job parentJob = updateParentOutputs(job);
            if (allChildJobCompleted(parentJob)) {
                completeParentJob(parentJob);
            }
         }
     }
    
    private Job updateParentOutputs(Job job) {
		return jobDataService.updateParentJob(job);
		
	}

    private void completeParentJob(Job parentJob) {
        // Wrap up the parent job
        parentJob.setStatus(Job.Status.COMPLETED);
        parentJob.setStage(JobStep.OUTPUT_LIST.getText());
        parentJob.setEndTime(LocalDateTime.now());
        parentJob.setGuiUrl(null);
        jobDataService.save(parentJob);
    }

    private boolean allChildJobCompleted(Job parentJob) {
    	parentJob = jobDataService.refreshFull(parentJob);
        return !parentJob.getSubJobs().stream().anyMatch(j -> j.getStatus() != Job.Status.COMPLETED);
    }

    private Multimap<String, String> listOutputFiles(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob,
            FstepWorkerGrpc.FstepWorkerBlockingStub worker, String outputRootPath) {
        FstepService service = job.getConfig().getService();

        OutputFileList outputFileList = worker.listOutputFiles(ListOutputFilesParam.newBuilder()
                .setJob(rpcJob).setOutputsRootPath(outputRootPath).build());
        List<String> relativePaths = outputFileList.getItemsList().stream()
                .map(OutputFileItem::getRelativePath).collect(toList());

        Multimap<String, String> outputsByRelativePath;
        if (service.getType() == FstepService.Type.APPLICATION) {
            outputsByRelativePath = IntStream.range(0, relativePaths.size()).boxed()
            .collect(ArrayListMultimap::create, (mm,i) -> mm.put(Integer.toString(i+1), relativePaths.get(i)), Multimap::putAll);
            
        } else {
            // Ensure we have one file per expected output
            Set<String> expectedServiceOutputIds = service.getServiceDescriptor().getDataOutputs()
                    .stream().map(FstepServiceDescriptor.Parameter::getId).collect(toSet());
            outputsByRelativePath = ArrayListMultimap.create();
            
            for (String expectedOutputId : expectedServiceOutputIds) {
                List<String> relativePathValues = relativePaths.stream()
                        .filter(path -> path.startsWith(expectedOutputId + "/"))
                        .collect(Collectors.toList());
                //TODO Check against user defined min/max occurs 
                //TODO Evaluate WPS compatibility issues with missing output
                if (relativePathValues.size() > 0) {
                    outputsByRelativePath.putAll(expectedOutputId, relativePathValues);
                } else {
                    try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                        LOG.info("Service defined output with ID '{}' but no matching directory was found in the job outputs", expectedOutputId);
                    }
                }
            }
        }
        return outputsByRelativePath;
    }

    private Multimap<String, FstepFile> repatriateAndIngestOutputFiles(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob,
            FstepWorkerGrpc.FstepWorkerBlockingStub worker, String outputRootPath,
            Multimap<String, String> outputsByRelativePath) throws IOException, InterruptedException {
        Multimap<String, FstepFile> outputFiles = ArrayListMultimap.create();
        FstepWorkerGrpc.FstepWorkerStub asyncWorker = FstepWorkerGrpc.newStub(worker.getChannel());
        for (String outputId : outputsByRelativePath.keySet()) {
        	for (String relativePath : outputsByRelativePath.get(outputId)) {
        		GetOutputFileParam getOutputFileParam = GetOutputFileParam.newBuilder().setJob(rpcJob)
                        .setPath(Paths.get(outputRootPath).resolve(relativePath).toString()).build();
        		FstepFile fstepFile = outputIngestionService.repatriateAndIngestOutputFile(job, outputId, Paths.get(relativePath), f -> asyncWorker.getOutputFile(getOutputFileParam, f));
        		outputFiles.put(outputId, fstepFile);
    		}
        }
        return outputFiles;
    }
}
