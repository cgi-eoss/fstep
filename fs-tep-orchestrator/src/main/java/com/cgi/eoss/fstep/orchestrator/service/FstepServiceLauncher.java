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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.jooq.lambda.Unchecked;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.catalogue.geoserver.GeoServerSpec;
import com.cgi.eoss.fstep.catalogue.util.GeoUtil;
import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.logging.Logging;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.JobStep;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor.Parameter;
import com.cgi.eoss.fstep.model.internal.OutputFileMetadata;
import com.cgi.eoss.fstep.model.internal.OutputProductMetadata;
import com.cgi.eoss.fstep.model.internal.RetrievedOutputFile;
import com.cgi.eoss.fstep.model.internal.OutputFileMetadata.OutputFileMetadataBuilder;
import com.cgi.eoss.fstep.model.internal.OutputProductMetadata.OutputProductMetadataBuilder;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.rpc.FileStream;
import com.cgi.eoss.fstep.rpc.FileStreamClient;
import com.cgi.eoss.fstep.rpc.FstepServiceLauncherGrpc;
import com.cgi.eoss.fstep.rpc.FstepServiceParams;
import com.cgi.eoss.fstep.rpc.FstepServiceResponse;
import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.JobParam;
import com.cgi.eoss.fstep.rpc.ListWorkersParams;
import com.cgi.eoss.fstep.rpc.StopServiceParams;
import com.cgi.eoss.fstep.rpc.StopServiceResponse;
import com.cgi.eoss.fstep.rpc.WorkersList;
import com.cgi.eoss.fstep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.fstep.rpc.worker.ExitParams;
import com.cgi.eoss.fstep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.rpc.worker.GetOutputFileParam;
import com.cgi.eoss.fstep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.fstep.rpc.worker.JobEnvironment;
import com.cgi.eoss.fstep.rpc.worker.JobInputs;
import com.cgi.eoss.fstep.rpc.worker.ListOutputFilesParam;
import com.cgi.eoss.fstep.rpc.worker.OutputFileItem;
import com.cgi.eoss.fstep.rpc.worker.OutputFileList;
import com.cgi.eoss.fstep.rpc.worker.PortBinding;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc.FstepWorkerBlockingStub;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.mysema.commons.lang.Pair;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;

/**
 * <p>Primary entry point for WPS services to launch in FS-TEP.</p>
 * <p>Provides access to FS-TEP data services and job distribution capability.</p>
 */
@Service
@Log4j2
@GRpcService
public class FstepServiceLauncher extends FstepServiceLauncherGrpc.FstepServiceLauncherImplBase {

    private static final String TIMEOUT_PARAM = "timeout";

    // TODO Synchronise this list at startup from workers
    private final Map<com.cgi.eoss.fstep.rpc.Job, FstepWorkerGrpc.FstepWorkerBlockingStub> jobWorkers = new HashMap<>();

    private final CachingWorkerFactory workerFactory;
    private final JobDataService jobDataService;
    private final FstepGuiServiceManager guiService;
    private final CatalogueService catalogueService;
    private final CostingService costingService;
    private final FstepSecurityService securityService;
    private final DynamicProxyService dynamicProxyService;
    
    @Autowired
    public FstepServiceLauncher(CachingWorkerFactory workerFactory, JobDataService jobDataService, FstepGuiServiceManager guiService, CatalogueService catalogueService, CostingService costingService, FstepSecurityService securityService, DynamicProxyService dynamicProxyService) {
        this.workerFactory = workerFactory;
        this.jobDataService = jobDataService;
        this.guiService = guiService;
        this.catalogueService = catalogueService;
        this.costingService = costingService;
        this.securityService = securityService;
        this.dynamicProxyService = dynamicProxyService;
    }

    @Override
    public void launchService(FstepServiceParams request, StreamObserver<FstepServiceResponse> responseObserver) {
        String zooId = request.getJobId();
        String userId = request.getUserId();
        String serviceId = request.getServiceId();
        String jobConfigLabel = request.getJobConfigLabel();
        List<JobParam> rpcInputs = request.getInputsList();
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);

        Job job = null;
        com.cgi.eoss.fstep.rpc.Job rpcJob = null;
        try (CloseableThreadContext.Instance ctc = CloseableThreadContext.push("FS-TEP Service Orchestrator")
                .put("userId", userId).put("serviceId", serviceId).put("zooId", zooId)) {
            // TODO Allow re-use of existing JobConfig
            job = jobDataService.buildNew(zooId, userId, serviceId, jobConfigLabel, inputs);
            rpcJob = GrpcUtil.toRpcJob(job);

            // Post back the job metadata for async responses
            responseObserver.onNext(FstepServiceResponse.newBuilder().setJob(rpcJob).build());

            ctc.put("jobId", String.valueOf(job.getId()));
            FstepService service = job.getConfig().getService();

            checkCost(job.getOwner(), job.getConfig());

            if (!checkInputs(job.getOwner(), rpcInputs)) {
                try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                    LOG.error("User {} does not have read access to all requested inputs", userId);
                }
                throw new ServiceExecutionException("User does not have read access to all requested inputs");
            }

            FstepWorkerGrpc.FstepWorkerBlockingStub worker = workerFactory.getWorker(job.getConfig());
            jobWorkers.put(rpcJob, worker);

            SetMultimap<String, FstepFile> jobOutputFiles = MultimapBuilder.hashKeys().hashSetValues().build();

            if (service.getType() == FstepService.Type.PARALLEL_PROCESSOR) {
                SetMultimap<String, FstepFile> parallelJobOutputs = executeParallelJob(job, rpcJob, rpcInputs, worker);
                jobOutputFiles.putAll(parallelJobOutputs);
            } else {
                Multimap<String, FstepFile> jobOutputs = executeJob(job, rpcJob, rpcInputs, worker);
                jobOutputs.forEach(jobOutputFiles::put);
            }

            chargeUser(job.getOwner(), job);

            // Transform the results for the WPS response
            List<JobParam> outputs = jobOutputFiles.asMap().entrySet().stream()
                    .map(e -> JobParam.newBuilder().setParamName(e.getKey()).addAllParamValue(e.getValue().stream().map(f -> f.getUri().toASCIIString()).collect(toSet())).build())
                    .collect(toList());

            responseObserver.onNext(FstepServiceResponse.newBuilder()
                    .setJobOutputs(FstepServiceResponse.JobOutputs.newBuilder().addAllOutputs(outputs).build())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            if (job != null) {
                job.setStatus(Job.Status.ERROR);
                job.setEndTime(LocalDateTime.now());
                jobDataService.save(job);
            }

            LOG.error("Failed to run processor; notifying gRPC client", e);
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        } finally {
            Optional.ofNullable(rpcJob).ifPresent(jobWorkers::remove);
        }
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

    @Override
    public void stopService(StopServiceParams request, StreamObserver<StopServiceResponse> responseObserver) {
        com.cgi.eoss.fstep.rpc.Job rpcJob = request.getJob();

        try {
            FstepWorkerGrpc.FstepWorkerBlockingStub worker = Optional.ofNullable(jobWorkers.get(rpcJob)).orElseThrow(() -> new IllegalStateException("FS-TEP worker not found for job " + rpcJob.getId()));
            LOG.info("Stop requested for job {}", rpcJob.getId());
            worker.stopContainer(rpcJob);
            LOG.info("Successfully stopped job {}", rpcJob.getId());
            responseObserver.onNext(StopServiceResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to stop job {}; notifying gRPC client", rpcJob.getId(), e);
            responseObserver.onError(new StatusRuntimeException(io.grpc.Status.fromCode(io.grpc.Status.Code.ABORTED).withCause(e)));
        } finally {
            jobWorkers.remove(rpcJob);
        }
    }

    private void checkCost(User user, JobConfig jobConfig) {
        int estimatedCost = costingService.estimateJobCost(jobConfig);
        if (estimatedCost > user.getWallet().getBalance()) {
            throw new ServiceExecutionException("Estimated cost (" + estimatedCost + " coins) exceeds current wallet balance");
        }
        // TODO Should estimated balance be "locked" in the wallet?
    }

    private boolean checkInputs(User user, List<JobParam> inputsList) {
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(inputsList);

        Set<URI> inputUris = inputs.entries().stream()
                .filter(e -> this.isValidUri(e.getValue()))
                .flatMap(e -> Arrays.stream(StringUtils.split(e.getValue(), ',')).map(URI::create))
                .collect(Collectors.toSet());

        return inputUris.stream().allMatch(uri -> catalogueService.canUserRead(user, uri));
    }

    private boolean isValidUri(String test) {
        try {
            return URI.create(test).getScheme() != null;
        } catch (Exception unused) {
            return false;
        }
    }

    private SetMultimap<String, FstepFile> executeParallelJob(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob, List<JobParam> rpcInputs, FstepWorkerGrpc.FstepWorkerBlockingStub worker) throws InterruptedException, ExecutionException, TimeoutException {
        String zooId = job.getExtId();
        String userId = job.getOwner().getName();
        FstepService service = job.getConfig().getService();
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);
        SetMultimap<String, FstepFile> jobOutputFiles = MultimapBuilder.hashKeys().hashSetValues().build();

        LOG.info("Launching parallel processing for job {}", zooId);
        job.setStartTime(LocalDateTime.now());
        job.setStatus(Job.Status.RUNNING);
        job.setStage(JobStep.PROCESSING.getText());
        jobDataService.save(job);

        // Split the magical "parallelInputs" attribute to get the individual job "input" parameters
        List<String> parallelInputs = inputs.get("parallelInputs").stream()
                .map(i -> Arrays.asList(i.split(",")))
                .flatMap(Collection::stream)
                .collect(toList());

        // Create the simpler map of parameters shared by all parallel jobs
        SetMultimap<String, String> sharedParams = MultimapBuilder.hashKeys().hashSetValues().build(inputs);
        sharedParams.removeAll("parallelInputs");

        ExecutorService executorService = Executors.newCachedThreadPool();
        ListeningExecutorService listeningExecutorService = MoreExecutors.listeningDecorator(executorService);
        List<ListenableFuture<?>> jobFutures = new ArrayList<>(parallelInputs.size());

        for (String parallelInput : parallelInputs) {
            SetMultimap<String, String> parallelJobParams = MultimapBuilder.hashKeys().hashSetValues().build(sharedParams);
            parallelJobParams.put("input", parallelInput);

            Job parallelJob = jobDataService.buildNew(UUID.randomUUID().toString(), userId, service.getName(), job.getConfig().getLabel(), parallelJobParams);
            com.cgi.eoss.fstep.rpc.Job parallelRpcJob = GrpcUtil.toRpcJob(parallelJob);
            List<JobParam> parallelRpcInputs = GrpcUtil.mapToParams(parallelJobParams);

            LOG.info("Launching child job {} ({}) for job {} ({})", parallelJob.getExtId(), parallelJob.getId(), job.getExtId(), job.getId());

            jobFutures.add(listeningExecutorService.submit(Unchecked.runnable(() -> {
                Multimap<String, FstepFile> parallelJobOutputs = executeJob(parallelJob, parallelRpcJob, parallelRpcInputs, worker);
                parallelJobOutputs.forEach(jobOutputFiles::put);
            })));
        }

        // Join and wait for the forked jobs
        LOG.info("Waiting for parallel child jobs to finish executing");
        Futures.allAsList(jobFutures).get(1, TimeUnit.DAYS);
        LOG.info("Parallel child jobs completed");

        // Wrap up the parent job
        job.setStatus(Job.Status.COMPLETED);
        job.setStage(JobStep.OUTPUT_LIST.getText());
        job.setEndTime(LocalDateTime.now());
        job.setGuiUrl(null);
        job.setGuiEndpoint(null);
        job.setOutputs(jobOutputFiles.entries().stream().collect(toMultimap(
                e -> e.getKey(),
                e -> e.getValue().getUri().toString(),
                MultimapBuilder.hashKeys().hashSetValues()::build)));
        job.setOutputFiles(ImmutableSet.copyOf(jobOutputFiles.values()));
        jobDataService.save(job);

        return jobOutputFiles;
    }

    private Multimap<String, FstepFile> executeJob(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob, List<JobParam> rpcInputs, FstepWorkerGrpc.FstepWorkerBlockingStub worker) throws Exception {
        String zooId = job.getExtId();
        FstepService service = job.getConfig().getService();
        Multimap<String, String> inputs = GrpcUtil.paramsListToMap(rpcInputs);

        // Create workspace and prepare inputs
        JobEnvironment jobEnvironment = prepareEnvironment(job, rpcJob, rpcInputs, worker);

        // Configure and launch the docker container
        launchContainer(job, rpcJob, worker, jobEnvironment);

        // TODO Implement async service command execution

        // Update GUI endpoint URL for client access
        if (service.getType() == FstepService.Type.APPLICATION) {
        	PortBinding portBinding = guiService.getGuiPortBinding(worker, rpcJob);
            ReverseProxyEntry guiEntry = dynamicProxyService.getProxyEntry(rpcJob, portBinding.getBinding().getIp(), portBinding.getBinding().getPort());
            LOG.info("Updating GUI URL for job {} ({}): {}", zooId,
                    job.getConfig().getService().getName(), guiEntry.getPath());
            job.setGuiUrl(guiEntry.getPath());
            jobDataService.save(job);
            dynamicProxyService.update();
        }

        // Wait for exit, with timeout if necessary
        waitForContainerExit(job, rpcJob, worker, inputs);

        // Enumerate files in the job output directory
        Multimap<String, String> outputsByRelativePath = listOutputFiles(job, rpcJob, worker, jobEnvironment);

        // Repatriate output files
        Multimap<String, FstepFile> outputFiles = repatriateAndIngestOutputFiles(job, rpcJob, worker, inputs, jobEnvironment, outputsByRelativePath);

        job.setStatus(Job.Status.COMPLETED);
        job.setOutputs(outputFiles.entries().stream()
                .collect(toMultimap(e -> e.getKey(), e -> e.getValue().getUri().toString(),
                        MultimapBuilder.hashKeys().hashSetValues()::build)));
        job.setOutputFiles(ImmutableSet.copyOf(outputFiles.values()));
        jobDataService.save(job);

        if (service.getType() == FstepService.Type.BULK_PROCESSOR) {
            // Auto-publish the output files
            ImmutableSet.copyOf(outputFiles.values()).forEach(f -> securityService.publish(FstepFile.class, f.getId()));
        }

        return outputFiles;
    }


    private JobEnvironment prepareEnvironment(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob, List<JobParam> rpcInputs, FstepWorkerGrpc.FstepWorkerBlockingStub worker) {
        LOG.info("Downloading input data for {}", job.getExtId());
        job.setStartTime(LocalDateTime.now());
        job.setStatus(Job.Status.RUNNING);
        job.setStage(JobStep.DATA_FETCH.getText());
        jobDataService.save(job);

        return worker.prepareEnvironment(JobInputs.newBuilder()
                .setJob(rpcJob)
                .addAllInputs(rpcInputs)
                .build());
    }

    private void launchContainer(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob, FstepWorkerGrpc.FstepWorkerBlockingStub worker, JobEnvironment jobEnvironment) {
        FstepService service = job.getConfig().getService();
        String dockerImageTag = service.getDockerTag();

        JobDockerConfig.Builder dockerConfigBuilder = JobDockerConfig.newBuilder()
                .setJob(rpcJob)
                .setServiceName(service.getName())
                .setDockerImage(dockerImageTag)
                .addBinds("/data:/data:ro")
                .addBinds(jobEnvironment.getWorkingDir() + "/FSTEP-WPS-INPUT.properties:" + "/home/worker/workDir/FSTEP-WPS-INPUT.properties:ro")
                .addBinds(jobEnvironment.getInputDir() + ":" + "/home/worker/workDir/inDir:ro")
                .addBinds(jobEnvironment.getOutputDir() + ":" + "/home/worker/workDir/outDir:rw");

        if (service.getType() == FstepService.Type.APPLICATION) {
            dockerConfigBuilder.addPorts(FstepGuiServiceManager.GUACAMOLE_PORT);
            if (dynamicProxyService.supportsProxyRoute()) {
            	dockerConfigBuilder.putEnvironmentVariables("PLATFORM_REVERSE_PROXY_PREFIX", dynamicProxyService.getProxyRoute(rpcJob));
        	}
        }
        
        LOG.info("Launching docker container for job {}", job.getExtId());
        job.setStage(JobStep.PROCESSING.getText());
        jobDataService.save(job);
        worker.launchContainer(dockerConfigBuilder.build());
        LOG.info("Job {} ({}) launched for service: {}", job.getId(), job.getExtId(), service.getName());
    }

    private void waitForContainerExit(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob, FstepWorkerGrpc.FstepWorkerBlockingStub worker, Multimap<String, String> inputs) {
        ContainerExitCode exitCode;
        if (inputs.containsKey(TIMEOUT_PARAM)) {
            int timeout = Integer.parseInt(Iterables.getOnlyElement(inputs.get(TIMEOUT_PARAM)));
            exitCode = worker.waitForContainerExitWithTimeout(ExitWithTimeoutParams.newBuilder().setJob(rpcJob).setTimeout(timeout).build());
        } else {
            exitCode = worker.waitForContainerExit(ExitParams.newBuilder().setJob(rpcJob).build());
        }

        switch (exitCode.getExitCode()) {
            case 0:
                // Normal exit
                break;
            case 137:
                LOG.info("Docker container for {} terminated via SIGKILL (exit code 137)", job.getExtId());
                break;
            case 143:
                LOG.info("Docker container for {} terminated via SIGTERM (exit code 143)", job.getExtId());
                break;
            default:
                throw new ServiceExecutionException("Docker container returned with exit code " + exitCode);
        }

        job.setStage(JobStep.OUTPUT_LIST.getText());
        job.setEndTime(LocalDateTime.now()); // End time is when processing ends
        job.setGuiUrl(null); // Any GUI services will no longer be available
        job.setGuiEndpoint(null);
        jobDataService.save(job);
    }

    private Multimap<String, String> listOutputFiles(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob,
            FstepWorkerGrpc.FstepWorkerBlockingStub worker, JobEnvironment jobEnvironment)
            throws Exception {
        FstepService service = job.getConfig().getService();

        OutputFileList outputFileList = worker.listOutputFiles(ListOutputFilesParam.newBuilder()
                .setJob(rpcJob).setOutputsRootPath(jobEnvironment.getOutputDir()).build());
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
                if (relativePathValues.size() > 0) {
                    outputsByRelativePath.putAll(expectedOutputId, relativePathValues);
                } else {
                    throw new Exception(String.format(
                            "Did not find a minimum of 1 output for '%s' in outputs list: %s",
                            expectedOutputId, relativePathValues));
                }
            }
        }
        return outputsByRelativePath;
    }

    private Multimap<String, FstepFile> repatriateAndIngestOutputFiles(Job job, com.cgi.eoss.fstep.rpc.Job rpcJob,
            FstepWorkerGrpc.FstepWorkerBlockingStub worker, Multimap<String, String> inputs, JobEnvironment jobEnvironment,
            Multimap<String, String> outputsByRelativePath) throws IOException, InterruptedException {
        List<RetrievedOutputFile> retrievedOutputFiles = new ArrayList<RetrievedOutputFile>(outputsByRelativePath.size());

        Multimap<String, FstepFile> outputFiles = ArrayListMultimap.create();
        Map<String, GeoServerSpec> geoServerSpecs = getGeoServerSpecs(inputs);
        Map<String, String> collectionSpecs = getCollectionSpecs(inputs);

        for (String outputId : outputsByRelativePath.keySet()) {
            OutputProductMetadata outputProduct = getOutputMetadata(job, geoServerSpecs, collectionSpecs, outputId);

            for (String relativePath : outputsByRelativePath.get(outputId)) {
                GetOutputFileParam getOutputFileParam = GetOutputFileParam.newBuilder().setJob(rpcJob)
                        .setPath(Paths.get(jobEnvironment.getOutputDir()).resolve(relativePath).toString()).build();

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

                        setOutputPath(catalogueService.provisionNewOutputProduct(outputProduct, fileMeta.getFilename()));
                        LOG.info("Writing output file for job {}: {}", job.getExtId(), getOutputPath());
                        return new BufferedOutputStream(Files.newOutputStream(getOutputPath(), CREATE, TRUNCATE_EXISTING, WRITE));
                    }

                    @Override
                    public void onCompleted() {
                        super.onCompleted();
                        Pair<OffsetDateTime, OffsetDateTime> startEndDateTimes = getStartEndDateTimes(outputId);
                        outputFileMetadata.setStartDateTime(startEndDateTimes.getFirst());
                        outputFileMetadata.setEndDateTime(startEndDateTimes.getSecond());
                        retrievedOutputFiles.add(new RetrievedOutputFile(outputFileMetadata, getOutputPath()));
                    }

					private Pair<OffsetDateTime, OffsetDateTime> getStartEndDateTimes(String outputId) {
						try {
	                        //Retrieve the parameter 
	                        Optional<Parameter> outputParameter = getServiceOutputParameter(outputId);
	                        if (outputParameter.isPresent()) {
		                        String regexp = outputParameter.get().getTimeRegexp();
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
	                        }
                        }
                        catch(RuntimeException e) {
                        	LOG.error("Unable to parse date from regexp");
                        }
						return new Pair<OffsetDateTime, OffsetDateTime> (null, null);
					}

					private Optional<Parameter> getServiceOutputParameter(String outputId) {
						return job.getConfig().getService().getServiceDescriptor().getDataOutputs().stream().filter(p -> p.getId().equals(outputId)).findFirst();
					}
					
					private OffsetDateTime parseOffsetDateTime(String startDateStr, LocalTime defaultTime) {
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd[[ ]['T']HHmm[ss][.SSS][XXX]]");
						TemporalAccessor temporalAccessor = formatter.parseBest(startDateStr, OffsetDateTime::from, LocalDate::from);
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
        return outputFiles;
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
        if (collectionSpecForOutput == null) {
            properties.put("collection", geoServerSpecForOutput);
        }
        

        OutputProductMetadata outputProduct = outputProductMetadataBuilder.productProperties(properties).build();
        return outputProduct;
    }

        
    private List<RetrievedOutputFile> postProcessOutputProducts(List<RetrievedOutputFile> retrievedOutputFiles) throws IOException {
        // Try to read CRS/AOI from all files - note that CRS/AOI may still be null after this
        retrievedOutputFiles.forEach(retrievedOutputFile -> {
            retrievedOutputFile.getOutputFileMetadata().setCrs(getOutputCrs(retrievedOutputFile.getPath()));
            retrievedOutputFile.getOutputFileMetadata().setGeometry(getOutputGeometry(retrievedOutputFile.getPath()));
        });

        return retrievedOutputFiles;
    }

    
    private Map<String, GeoServerSpec> getGeoServerSpecs(Multimap<String, String> inputs) throws JsonParseException, JsonMappingException, IOException {
        String geoServerSpecsStr = Iterables.getOnlyElement(inputs.get("geoServerSpec"), null);
        Map<String, GeoServerSpec> geoServerSpecs = new HashMap<String, GeoServerSpec>();
        if (geoServerSpecsStr != null && geoServerSpecsStr.length() > 0) {
            ObjectMapper mapper = new ObjectMapper();
                TypeFactory typeFactory = mapper.getTypeFactory();
                MapType mapType = typeFactory.constructMapType(HashMap.class, String.class, GeoServerSpec.class);
                geoServerSpecs.putAll(mapper.readValue(geoServerSpecsStr, mapType));
        }
        return geoServerSpecs;
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
