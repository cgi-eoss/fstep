package com.cgi.eoss.fstep.orchestrator.service;

import static com.google.common.collect.Multimaps.toMultimap;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.logging.log4j.CloseableThreadContext;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.fstep.catalogue.util.GeoUtil;
import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.logging.Logging;
import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.model.CostQuotation.Recurrence;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepFilesRelation;
import com.cgi.eoss.fstep.model.FstepFilesRelation.Type;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor.Parameter;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor.Relation;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobProcessing;
import com.cgi.eoss.fstep.model.JobStep;
import com.cgi.eoss.fstep.model.WalletTransaction;
import com.cgi.eoss.fstep.model.internal.OutputFileMetadata;
import com.cgi.eoss.fstep.model.internal.OutputFileMetadata.OutputFileMetadataBuilder;
import com.cgi.eoss.fstep.model.internal.OutputProductMetadata;
import com.cgi.eoss.fstep.model.internal.OutputProductMetadata.OutputProductMetadataBuilder;
import com.cgi.eoss.fstep.model.internal.ParameterRelationTypeToFileRelationTypeUtil;
import com.cgi.eoss.fstep.model.internal.RetrievedOutputFile;
import com.cgi.eoss.fstep.persistence.service.FstepFilesRelationDataService;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.persistence.service.JobProcessingDataService;
import com.cgi.eoss.fstep.persistence.service.WalletTransactionDataService;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.rpc.FileStream;
import com.cgi.eoss.fstep.rpc.FileStreamClient;
import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.worker.ContainerExit;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc.FstepWorkerBlockingStub;
import com.cgi.eoss.fstep.rpc.worker.GetOutputFileParam;
import com.cgi.eoss.fstep.rpc.worker.JobError;
import com.cgi.eoss.fstep.rpc.worker.JobEvent;
import com.cgi.eoss.fstep.rpc.worker.JobEventType;
import com.cgi.eoss.fstep.rpc.worker.ListOutputFilesParam;
import com.cgi.eoss.fstep.rpc.worker.OutputFileItem;
import com.cgi.eoss.fstep.rpc.worker.OutputFileList;
import com.cgi.eoss.fstep.rpc.worker.PortBinding;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.protobuf.Timestamp;
import com.mysema.commons.lang.Pair;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class FstepJobUpdatesManager {
	
	private final JobDataService jobDataService;
    private final DynamicProxyService dynamicProxyService;
    private final FstepGuiServiceManager guiService;
    private final CachingWorkerFactory workerFactory;
    private final CatalogueService catalogueService;
    private final FstepSecurityService securityService;
    private final JobProcessingDataService jobProcessingDataService;
    private final CostingService costingService;
    private final WalletTransactionDataService walletTransactionDataService;
    private final FstepFilesRelationDataService fileRelationDataService;
	private final PlatformParameterExtractor platformParameterExtractor;
	 @Autowired
	    public FstepJobUpdatesManager(JobDataService jobDataService, 
	    		DynamicProxyService dynamicProxyService, 
	    		FstepGuiServiceManager guiService, 
	    		CachingWorkerFactory workerFactory,
	    		CatalogueService catalogueService,
	    		FstepSecurityService securityService,
	    		JobProcessingDataService jobProcessingDataService,
	    		CostingService costingService, 
	    		WalletTransactionDataService walletTransactionDataService,
	    		FstepFilesRelationDataService fileRelationDataService) {
	        this.jobDataService = jobDataService;
	        this.dynamicProxyService = dynamicProxyService;
	        this.guiService = guiService;
	        this.workerFactory = workerFactory;
	        this.catalogueService = catalogueService;
	        this.securityService = securityService;
	        this.jobProcessingDataService = jobProcessingDataService;
	        this.costingService = costingService;
	        this.walletTransactionDataService = walletTransactionDataService;
	        this.fileRelationDataService = fileRelationDataService;
	        this.platformParameterExtractor = new PlatformParameterExtractor();
	    }

    @JmsListener(destination = FstepQueueService.jobUpdatesQueueName)
    public void receiveJobUpdateMessage(@Payload ObjectMessage objectMessage, @Header("workerId") String workerId,
            @Header("jobId") String internalJobId) {
        try {
        	// TODO change into Chain of Responsibility type pattern
        	Serializable update = objectMessage.getObject();
            receiveJobUpdate(update, workerId, internalJobId);
        } catch (JMSException e) {
        	Job job = jobDataService.refreshFull(Long.parseLong(internalJobId));
            onJobError(job, e);
        }

    }
    
    public void receiveJobUpdate(Object update, String workerId, String internalJobId) {
        Job job = jobDataService.refreshFull(Long.parseLong(internalJobId));
        if (update instanceof JobEvent) {
            JobEvent jobEvent = (JobEvent) update;
            JobEventType jobEventType = jobEvent.getJobEventType();
            if (jobEventType == JobEventType.DATA_FETCHING_STARTED) {
                onJobDataFetchingStarted(job, workerId);
            } else if (jobEventType == JobEventType.DATA_FETCHING_COMPLETED) {
                onJobDataFetchingCompleted(job);
            } else if (jobEventType == JobEventType.PROCESSING_STARTED) {
                onJobProcessingStarted(job, workerId, jobEvent.getTimestamp());
            }
            else if (jobEventType == JobEventType.HEARTBEAT) {
                onJobHeartbeat(job, workerId, jobEvent.getTimestamp());
            }
        } else if (update instanceof JobError) {
            JobError jobError = (JobError) update;
            onJobError(job, jobError.getErrorDescription());
        } else if (update instanceof ContainerExit) {
            ContainerExit containerExit = (ContainerExit) update;
            try {
                onContainerExit(job, workerId, containerExit.getOutputRootPath(),
                        containerExit.getExitCode(), containerExit.getTimestamp());
            } catch (Exception e) {
                onJobError(job, e);
            }
        }
    }

    private void onJobHeartbeat(Job job, String workerId, Timestamp timestamp) {
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
				long hoursToCharge = hoursWithNewHeartbeat - hoursWithLastHeartbeat;
				for (long i = 0; i < hoursToCharge; i++) {
					costingService.chargeForJobProcessing(job.getOwner().getWallet(), jobProcessing);
				}
			}
		}
    	jobProcessing.setLastHeartbeat(newHeartbeat);
    	jobProcessingDataService.save(jobProcessing);
    }

	private void onJobDataFetchingStarted(Job job, String workerId) {
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

    private void onJobDataFetchingCompleted(Job job) {
        LOG.info("Launching docker container for job {}", job.getExtId());
    }

    private void onJobProcessingStarted(Job job, String workerId, Timestamp timestamp) {
        FstepService service = job.getConfig().getService();
        LOG.info("Job {} ({}) launched for service: {}", job.getId(), job.getExtId(),
                service.getName());
        // Update GUI endpoint URL for client access
        if (service.getType() == FstepService.Type.APPLICATION) {
            String zooId = job.getExtId();
            FstepWorkerBlockingStub worker = workerFactory.getWorkerById(workerId);
            com.cgi.eoss.fstep.rpc.Job rpcJob = GrpcUtil.toRpcJob(job);
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

    private void onContainerExit(Job job, String workerId, String outputRootPath,
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
            ingestOutput(job, GrpcUtil.toRpcJob(job), worker, outputRootPath);
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
	    	long runtimeMinutes = totalRuntime.toMinutes();
			int numHours = (int) (runtimeMinutes/60);
	    	if (runtimeMinutes % 60  != 0) {
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

	private void onJobError(Job job, String description) {
        LOG.error("Error in Job {}: {}",
                job.getExtId(), description);
        endJobWithError(job);
    }

    private void onJobError(Job job, Throwable t) {
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
            FstepWorkerBlockingStub worker, String outputRootPath)
            throws IOException, Exception {
        // Enumerate files in the job output directory
        Multimap<String, String> outputsByRelativePath =
                listOutputFiles(job, rpcJob, worker, outputRootPath);
        // Repatriate output files
        Multimap<String, FstepFile> outputFiles = repatriateAndIngestOutputFiles(job, rpcJob, worker,
                job.getConfig().getInputs(), outputRootPath, outputsByRelativePath);
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
            FstepWorkerGrpc.FstepWorkerBlockingStub worker, String outputRootPath)
            throws Exception {
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
            FstepWorkerGrpc.FstepWorkerBlockingStub worker, Multimap<String, String> inputs, String outputRootPath,
            Multimap<String, String> outputsByRelativePath) throws IOException, InterruptedException {
        List<RetrievedOutputFile> retrievedOutputFiles = new ArrayList<RetrievedOutputFile>(outputsByRelativePath.size());

        Multimap<String, FstepFile> outputFiles = ArrayListMultimap.create();
        Map<String, GeoServerSpec> geoServerSpecs = platformParameterExtractor.getGeoServerSpecs(job);
        Map<String, String> collectionSpecs = platformParameterExtractor.getCollectionSpecs(job);

        for (String outputId : outputsByRelativePath.keySet()) {
            OutputProductMetadata outputProduct = getOutputMetadata(job, geoServerSpecs, collectionSpecs, outputId);

            for (String relativePath : outputsByRelativePath.get(outputId)) {
                GetOutputFileParam getOutputFileParam = GetOutputFileParam.newBuilder().setJob(rpcJob)
                        .setPath(Paths.get(outputRootPath).resolve(relativePath).toString()).build();

                FstepWorkerGrpc.FstepWorkerStub asyncWorker = FstepWorkerGrpc.newStub(worker.getChannel());

                try (FileStreamClient<GetOutputFileParam> fileStreamClient = new FileStreamClient<GetOutputFileParam>() {
                    private OutputFileMetadata outputFileMetadata;

                    @Override
                    public OutputStream buildOutputStream(FileStream.FileMeta fileMeta) throws IOException {
                        LOG.info("Collecting output '{}' with filename {} ({} bytes)", outputId, fileMeta.getFilename(),
                                fileMeta.getSize());

                        OutputFileMetadataBuilder outputFileMetadataBuilder = OutputFileMetadata.builder();

                        outputFileMetadata = outputFileMetadataBuilder.outputProductMetadata(outputProduct)
                                .build();

                        setOutputPath(catalogueService.provisionNewOutputProduct(outputProduct, relativePath.toString()));
                        LOG.info("Writing output file for job {}: {}", job.getExtId(), getOutputPath());
                        return new BufferedOutputStream(Files.newOutputStream(getOutputPath(), CREATE, TRUNCATE_EXISTING, WRITE));
                    }

                    @Override
                    public void onCompleted() {
                        super.onCompleted();
                        Pair<OffsetDateTime, OffsetDateTime> startEndDateTimes = getServiceOutputParameter(job.getConfig().getService(), outputId).map(this::extractStartEndDateTimes)
                                .orElseGet(() -> new Pair<>(null, null));
                        outputFileMetadata.setStartDateTime(startEndDateTimes.getFirst());
                        outputFileMetadata.setEndDateTime(startEndDateTimes.getSecond());
                        retrievedOutputFiles.add(new RetrievedOutputFile(outputFileMetadata, getOutputPath()));
                    }

					private Pair<OffsetDateTime, OffsetDateTime> extractStartEndDateTimes(Parameter outputParameter) {
						try {
                            String regexp = outputParameter.getTimeRegexp();
	                        if (regexp != null) {
	                        	Pattern p = Pattern.compile(regexp);
	                        	Matcher m = p.matcher(getOutputPath().getFileName().toString());
	                        	if (m.find()) {
	                        		if (regexp.contains("?<startEnd>")) {
	                        			OffsetDateTime startEndDateTime = parseOffsetDateTime(m.group("startEnd"), LocalTime.MIDNIGHT);
	                        			return new Pair<OffsetDateTime, OffsetDateTime>(startEndDateTime, startEndDateTime);
	                        		}
	                        		else {
	                        			OffsetDateTime start = null, end = null;
	                        			if (regexp.contains("?<start>")) {
	                            			start = parseOffsetDateTime(m.group("start"), LocalTime.MIDNIGHT);
	                            		}
	                        			
	                        			if (regexp.contains("?<end>")) {
	                            			end = parseOffsetDateTime(m.group("end"), LocalTime.MIDNIGHT);
	                            		}
	                        			return new Pair<OffsetDateTime, OffsetDateTime>(start, end);
	                        		}
	                            }
	                        }
	                    } catch(RuntimeException e) {
                        	LOG.error("Unable to parse date from regexp");
                        }
						return new Pair<OffsetDateTime, OffsetDateTime> (null, null);
					}
					
					private OffsetDateTime parseOffsetDateTime(String startDateStr, LocalTime defaultTime) {
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd[[ ]['T']HHmm[ss][.SSS][XXX][VV]]");
						TemporalAccessor temporalAccessor = formatter.parseBest(startDateStr, OffsetDateTime::from, LocalDateTime::from, LocalDate::from);
						if (temporalAccessor instanceof OffsetDateTime) {
							return (OffsetDateTime) temporalAccessor;
						} 
						else if (temporalAccessor instanceof LocalDateTime){
							return ((LocalDateTime) temporalAccessor).atOffset(ZoneOffset.UTC);
						} 
						else
						{
							return ((LocalDate) temporalAccessor).atTime(defaultTime).atOffset(ZoneOffset.UTC);
						}
					}
                }) {
                    asyncWorker.getOutputFile(getOutputFileParam, fileStreamClient.getFileStreamObserver());
                    fileStreamClient.getLatch().await();
                }
            }
        }
        postProcessOutputProducts(retrievedOutputFiles).forEach( Unchecked.consumer(retrievedOutputFile -> outputFiles.put(retrievedOutputFile.getOutputFileMetadata().getOutputProductMetadata().getOutputId(), catalogueService.ingestOutputProduct(retrievedOutputFile.getOutputFileMetadata(), retrievedOutputFile.getPath()))));
        buildOutputFileRelations(outputFiles, job.getConfig().getService());
        
        return outputFiles;
    }
    
    private Optional<Parameter> getServiceOutputParameter(FstepService service, String outputId) {
		return service.getServiceDescriptor().getDataOutputs().stream().filter(p -> p.getId().equals(outputId)).findFirst();
	}
    

    private void buildOutputFileRelations(Multimap<String, FstepFile> outputFiles, FstepService service) {
		if (service.getServiceDescriptor().getDataOutputs() == null) {
			return;
		}
    	for ( Parameter output: service.getServiceDescriptor().getDataOutputs()) {
			if (output.getParameterRelations() != null){
				for (Relation parameterRelation: output.getParameterRelations()) {
					Collection<FstepFile> filesForSourceOutput = outputFiles.get(output.getId());
					Collection<FstepFile> filesForTargetOutput = outputFiles.get(parameterRelation.getTargetParameterId());
					for (FstepFile sourceFile: filesForSourceOutput) {
						for (FstepFile targetFile: filesForTargetOutput) {
							Type relationType = ParameterRelationTypeToFileRelationTypeUtil.fromParameterRelationType(parameterRelation.getType());
							FstepFilesRelation relation = new FstepFilesRelation(sourceFile, targetFile, relationType);
							fileRelationDataService.save(relation);
						}
					}
				}
			}
		}
	}
    

    private OutputProductMetadata getOutputMetadata(Job job, Map<String, GeoServerSpec> geoServerSpecs,
            Map<String, String> collectionSpecs, String outputId) {
        OutputProductMetadataBuilder outputProductMetadataBuilder = OutputProductMetadata.builder()
                .owner(job.getOwner())
                .service(job.getConfig().getService())
                .outputId(outputId)
                .jobId(job.getExtId());
                
        
        HashMap<String, Object> properties = new HashMap<>(ImmutableMap.<String, Object>builder()
                .put("jobId", job.getExtId()).put("intJobId", job.getId())
                .put("serviceName", job.getConfig().getService().getName())
                .put("jobOwner", job.getOwner().getName())
                .put("jobStartTime",
                        job.getStartTime().atOffset(ZoneOffset.UTC).toString())
                .put("jobEndTime", job.getEndTime().atOffset(ZoneOffset.UTC).toString())
                .build());
        
        GeoServerSpec geoServerSpecForOutput = geoServerSpecs.get(outputId);
        if (geoServerSpecForOutput != null) {
            properties.put("geoServerSpec", geoServerSpecForOutput);
        }
        
        String collectionSpecForOutput = collectionSpecs.get(outputId);
        if (collectionSpecForOutput != null) {
            properties.put("collection", collectionSpecForOutput);
        }
        
        getServiceOutputParameter(job.getConfig().getService(), outputId).ifPresent(p -> addPlatformMetadata(properties, p));
        
        OutputProductMetadata outputProduct = outputProductMetadataBuilder.productProperties(properties).build();
        return outputProduct;
    }

    private void addPlatformMetadata(Map<String, Object> properties, Parameter outputParameter) {
	    if (outputParameter.getPlatformMetadata() != null && outputParameter.getPlatformMetadata().size() > 0 ) {
	        properties.put("extraParams", outputParameter.getPlatformMetadata());
        }
	}
    
    private List<RetrievedOutputFile> postProcessOutputProducts(List<RetrievedOutputFile> retrievedOutputFiles) throws IOException {
        // Try to read CRS/AOI from all files - note that CRS/AOI may still be null after this
        retrievedOutputFiles.forEach(retrievedOutputFile -> {
            retrievedOutputFile.getOutputFileMetadata().setCrs(getOutputCrs(retrievedOutputFile.getPath()));
            retrievedOutputFile.getOutputFileMetadata().setGeometry(getOutputGeometry(retrievedOutputFile.getPath()));
        });

        return retrievedOutputFiles;
    }
    

    private String getOutputCrs(Path outputPath) {
        try {
            return GeoUtil.extractEpsg(outputPath);
        } catch (Exception e) {
        	LOG.info("Could not extract bounding box from file: {}", outputPath);
            return null;
        }
    }

    private String getOutputGeometry(Path outputPath) {
        try {
            return GeoUtil.geojsonToWkt(GeoUtil.extractBoundingBox(outputPath));
        } catch (Exception e) {
        	LOG.info("Could not extract bounding box from file: {}", outputPath);
            return null;
        }
    }

}
