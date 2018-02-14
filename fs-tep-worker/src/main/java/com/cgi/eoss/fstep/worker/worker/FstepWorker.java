package com.cgi.eoss.fstep.worker.worker;

import com.cgi.eoss.fstep.clouds.service.Node;
import com.cgi.eoss.fstep.clouds.service.NodeProvisioningException;
import com.cgi.eoss.fstep.io.ServiceInputOutputManager;
import com.cgi.eoss.fstep.io.ServiceIoException;
import com.cgi.eoss.fstep.logging.Logging;
import com.cgi.eoss.fstep.rpc.FileStream;
import com.cgi.eoss.fstep.rpc.GrpcUtil;
import com.cgi.eoss.fstep.rpc.Job;
import com.cgi.eoss.fstep.rpc.worker.Binding;
import com.cgi.eoss.fstep.rpc.worker.ContainerExitCode;
import com.cgi.eoss.fstep.rpc.worker.DockerImageConfig;
import com.cgi.eoss.fstep.rpc.worker.ExitParams;
import com.cgi.eoss.fstep.rpc.worker.ExitWithTimeoutParams;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.rpc.worker.GetOutputFileParam;
import com.cgi.eoss.fstep.rpc.worker.JobDockerConfig;
import com.cgi.eoss.fstep.rpc.worker.JobEnvironment;
import com.cgi.eoss.fstep.rpc.worker.JobInputs;
import com.cgi.eoss.fstep.rpc.worker.LaunchContainerResponse;
import com.cgi.eoss.fstep.rpc.worker.ListOutputFilesParam;
import com.cgi.eoss.fstep.rpc.worker.OutputFileItem;
import com.cgi.eoss.fstep.rpc.worker.OutputFileList;
import com.cgi.eoss.fstep.rpc.worker.PortBinding;
import com.cgi.eoss.fstep.rpc.worker.PortBindings;
import com.cgi.eoss.fstep.rpc.worker.PrepareDockerImageResponse;
import com.cgi.eoss.fstep.rpc.worker.StopContainerResponse;
import com.cgi.eoss.fstep.worker.DockerRegistryConfig;
import com.cgi.eoss.fstep.worker.docker.DockerClientFactory;
import com.cgi.eoss.fstep.worker.docker.Log4jContainerCallback;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;
import org.jooq.lambda.Unchecked;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import shadow.dockerjava.com.github.dockerjava.api.DockerClient;
import shadow.dockerjava.com.github.dockerjava.api.command.BuildImageCmd;
import shadow.dockerjava.com.github.dockerjava.api.command.CreateContainerCmd;
import shadow.dockerjava.com.github.dockerjava.api.command.InspectContainerResponse;
import shadow.dockerjava.com.github.dockerjava.api.command.PullImageCmd;
import shadow.dockerjava.com.github.dockerjava.api.command.PushImageCmd;
import shadow.dockerjava.com.github.dockerjava.api.exception.DockerClientException;
import shadow.dockerjava.com.github.dockerjava.api.exception.NotFoundException;
import shadow.dockerjava.com.github.dockerjava.api.model.AuthConfig;
import shadow.dockerjava.com.github.dockerjava.api.model.Bind;
import shadow.dockerjava.com.github.dockerjava.api.model.ExposedPort;
import shadow.dockerjava.com.github.dockerjava.api.model.Image;
import shadow.dockerjava.com.github.dockerjava.api.model.Ports;
import shadow.dockerjava.com.github.dockerjava.core.command.BuildImageResultCallback;
import shadow.dockerjava.com.github.dockerjava.core.command.PullImageResultCallback;
import shadow.dockerjava.com.github.dockerjava.core.command.PushImageResultCallback;
import shadow.dockerjava.com.github.dockerjava.core.command.WaitContainerResultCallback;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/**
 * <p>Service for executing FS-TEP (WPS) services inside Docker containers.</p>
 */
@GRpcService
@Log4j2
public class FstepWorker extends FstepWorkerGrpc.FstepWorkerImplBase {
    
    private FstepWorkerNodeManager nodeManager;
    private final JobEnvironmentService jobEnvironmentService;
    private final ServiceInputOutputManager inputOutputManager;
    
    // Track which DockerClient is used for each job
    private final Map<String, DockerClient> jobClients = new HashMap<>();
    // Track which container ID is used for each job
    private final Map<String, String> jobContainers = new HashMap<>();
    // Track which input URIs are used for each job
    private final Multimap<String, URI> jobInputs = MultimapBuilder.hashKeys().hashSetValues().build();

    private int minWorkerNodes;

    private DockerRegistryConfig dockerRegistryConfig;
    
    
    @Autowired
    public FstepWorker(FstepWorkerNodeManager nodeManager, JobEnvironmentService jobEnvironmentService,
            ServiceInputOutputManager inputOutputManager, @Qualifier("minWorkerNodes") int minWorkerNodes) {
        this.nodeManager = nodeManager;
        this.jobEnvironmentService = jobEnvironmentService;
        this.inputOutputManager = inputOutputManager;
        this.minWorkerNodes = minWorkerNodes;
    }
    
    @Autowired(required = false)
    public void setDockerRegistryConfig(DockerRegistryConfig dockerRegistryConfig) {
        this.dockerRegistryConfig = dockerRegistryConfig;
    }
    
    @PostConstruct
    public void allocateMinNodes() {
        int currentNodes = nodeManager.getCurrentNodes(FstepWorkerNodeManager.pooledWorkerTag).size();
        if (currentNodes < minWorkerNodes) {
            try {
                nodeManager.provisionNodes(minWorkerNodes - currentNodes, FstepWorkerNodeManager.pooledWorkerTag, jobEnvironmentService.getBaseDir());
            } catch (NodeProvisioningException e) {
                LOG.error("Failed initial node provisioning: {}", e.getMessage());
            }
        }
    }
 
    
    @Override
    public void prepareEnvironment(JobInputs request, StreamObserver<JobEnvironment> responseObserver) {
            try {
                nodeManager.provisionNodeForJob(jobEnvironmentService.getBaseDir(), request.getJob().getId());
            } catch (NodeProvisioningException e) {
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        prepareInputs(request, responseObserver);
    }
    
    @Override
    public void prepareInputs(JobInputs request, StreamObserver<JobEnvironment> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            try {
                Node node = nodeManager.getJobNode(request.getJob().getId());
                DockerClient dockerClient;
                if (dockerRegistryConfig != null) {
                    dockerClient = DockerClientFactory.buildDockerClient(node.getDockerEngineUrl(), 
                        dockerRegistryConfig);
                }
                else {
                    dockerClient = DockerClientFactory.buildDockerClient(node.getDockerEngineUrl());
                }
                jobClients.put(request.getJob().getId(), dockerClient);
            } catch (Exception e) {
                try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                    LOG.error("Failed to prepare Docker context: {}", e.getMessage());
                }
                LOG.error("Failed to prepare Docker context for {}", request.getJob().getId(), e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }

            try {
                Multimap<String, String> inputs = GrpcUtil.paramsListToMap(request.getInputsList());

                // Create workspace directories and input parameters file
                com.cgi.eoss.fstep.worker.worker.JobEnvironment jobEnv = jobEnvironmentService.createEnvironment(request.getJob().getId(), inputs);

                // Resolve and download any URI-type inputs
                for (Map.Entry<String, String> e : inputs.entries()) {
                    if (isValidUri(e.getValue())) {
                        Path subdirPath = jobEnv.getInputDir().resolve(e.getKey());

                        // Just hope no one has used a comma in their url...
                        Set<URI> inputUris = Arrays.stream(StringUtils.split(e.getValue(), ',')).map(URI::create).collect(Collectors.toSet());
                        inputOutputManager.prepareInput(subdirPath, inputUris);
                        jobInputs.putAll(request.getJob().getId(), inputUris);
                    }
                }

                JobEnvironment ret = JobEnvironment.newBuilder()
                        .setInputDir(jobEnv.getInputDir().toAbsolutePath().toString())
                        .setOutputDir(jobEnv.getOutputDir().toAbsolutePath().toString())
                        .setWorkingDir(jobEnv.getWorkingDir().toAbsolutePath().toString())
                        .build();

                responseObserver.onNext(ret);
                responseObserver.onCompleted();
            } catch (Exception e) {
                try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                    LOG.error("Failed to prepare job inputs: {}", e.getMessage());
                }
                LOG.error("Failed to prepare job inputs for {}", request.getJob().getId(), e);
                cleanUpJob(request.getJob().getId());
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    
   @Override
   public void prepareDockerImage(DockerImageConfig request, StreamObserver<PrepareDockerImageResponse> responseObserver) {
        DockerClient dockerClient;
        if (dockerRegistryConfig != null) {
            dockerClient = DockerClientFactory.buildDockerClient("unix:///var/run/docker.sock", dockerRegistryConfig);
            try {
                String dockerImageTag = dockerRegistryConfig.getDockerRegistryUrl() + "/"+request.getDockerImage();
                responseObserver.onNext(PrepareDockerImageResponse.newBuilder().build());
                //Remove previous images with same name
                removeDockerImage(dockerClient, dockerImageTag);
                buildDockerImage(dockerClient, request.getServiceName(), dockerImageTag);
                pushDockerImage(dockerClient, dockerImageTag);
                dockerClient.close();
                responseObserver.onCompleted();
            } catch (IOException e) {
                responseObserver.onError(e);
            } catch (InterruptedException e) {
                responseObserver.onError(e);
            }
        }
        else {
            String errorMessage = "No docker registry available to prepare the Docker image";
            LOG.error(errorMessage);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(new Exception(errorMessage))));
        }
    }
    
    @Override
    public void launchContainer(JobDockerConfig request, StreamObserver<LaunchContainerResponse> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            Preconditions.checkArgument(jobClients.containsKey(request.getJob().getId()), "Job ID %s is not attached to a DockerClient", request.getJob().getId());

            DockerClient dockerClient = jobClients.get(request.getJob().getId());
            String containerId = null;

            try {
                String imageTag;
                if (dockerRegistryConfig != null) {
                    imageTag = dockerRegistryConfig.getDockerRegistryUrl() + "/" + request.getDockerImage();
                    //Get the image from the repository, if available
                    try {
                        pullDockerImage(dockerClient, imageTag);
                    }
                    catch(DockerClientException | NotFoundException e) {
                        LOG.info("Failed to pull image {} from registry '{}'", imageTag, dockerRegistryConfig.getDockerRegistryUrl());
                    }
                }
                
                else {
                    imageTag = request.getDockerImage();
                }
                
                if (!isImageAvailableLocally(dockerClient, imageTag)) {
                    LOG.info("Building image '{}' locally", imageTag);
                    buildDockerImage(dockerClient, request.getServiceName(), imageTag);
                }
                
                // Launch tag
                try (CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(imageTag)) {
                    createContainerCmd.withLabels(ImmutableMap.of(
                            "jobId", request.getJob().getId(),
                            "intJobId", request.getJob().getIntJobId(),
                            "userId", request.getJob().getUserId(),
                            "serviceId", request.getJob().getServiceId()
                    ));
                    createContainerCmd.withBinds(request.getBindsList().stream().map(Bind::parse).collect(Collectors.toList()));
                    createContainerCmd.withExposedPorts(request.getPortsList().stream().map(ExposedPort::parse).collect(Collectors.toList()));
                    createContainerCmd.withPortBindings(request.getPortsList().stream()
                            .map(p -> new shadow.dockerjava.com.github.dockerjava.api.model.PortBinding(new Ports.Binding(null, null), ExposedPort.parse(p)))
                            .collect(Collectors.toList()));

                    // Add proxy vars to the container, if they are set in the environment
                    createContainerCmd.withEnv(
                            ImmutableSet.of("http_proxy", "https_proxy", "no_proxy").stream()
                                    .filter(var -> System.getenv().containsKey(var))
                                    .map(var -> var + "=" + System.getenv(var))
                                    .collect(Collectors.toList()));

                    containerId = createContainerCmd.exec().getId();
                    jobContainers.put(request.getJob().getId(), containerId);
                }

                LOG.info("Launching container {} for job {}", containerId, request.getJob().getId());
                dockerClient.startContainerCmd(containerId).exec();

                dockerClient.logContainerCmd(containerId).withStdErr(true).withStdOut(true).withFollowStream(true).withTailAll()
                        .exec(new Log4jContainerCallback());

                responseObserver.onNext(LaunchContainerResponse.newBuilder().build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
                    LOG.error("Failed to launch Docker container: {}", e.getMessage());
                }
                LOG.error("Failed to launch Docker container {}", request.getDockerImage(), e);
                if (!Strings.isNullOrEmpty(containerId)) {
                    removeContainer(dockerClient, containerId);
                    cleanUpJob(request.getJob().getId());
                }
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    @Override
    public void getPortBindings(Job request, StreamObserver<PortBindings> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request)) {
            Preconditions.checkArgument(jobClients.containsKey(request.getId()), "Job ID %s is not attached to a DockerClient", request.getId());
            Preconditions.checkArgument(jobContainers.containsKey(request.getId()), "Job ID %s does not have a known container ID", request.getId());

            DockerClient dockerClient = jobClients.get(request.getId());
            String containerId = jobContainers.get(request.getId());
            try {
                LOG.debug("Inspecting container for port bindings: {}", containerId);
                InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();
                Map<ExposedPort, Ports.Binding[]> exposedPortMap = inspectContainerResponse.getNetworkSettings().getPorts().getBindings();

                LOG.debug("Returning port map: {}", exposedPortMap);
                
                String nodeIpAddress = null;
                Node jobNode = nodeManager.getJobNode(request.getId());
                if (jobNode != null && jobNode.getIpAddress() != null) {
                    nodeIpAddress = jobNode.getIpAddress();
                }
                
                final String ipAddress = nodeIpAddress;
                
                PortBindings.Builder bindingsBuilder = PortBindings.newBuilder();
                exposedPortMap.entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> PortBinding.newBuilder()
                                .setPortDef(e.getKey().toString())
                                .setBinding(Binding.newBuilder().setIp((ipAddress != null)?ipAddress:e.getValue()[0].getHostIp()).setPort(Integer.parseInt(e.getValue()[0].getHostPortSpec())).build())
                                .build())
                        .forEach(bindingsBuilder::addBindings);

                responseObserver.onNext(bindingsBuilder.build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                if (!Strings.isNullOrEmpty(containerId)) {
                    removeContainer(dockerClient, containerId);
                    cleanUpJob(request.getId());
                }
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }
    
    @Override
    public void stopContainer(Job request, StreamObserver<StopContainerResponse> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request)) {
            Preconditions.checkArgument(jobClients.containsKey(request.getId()), "Job ID %s is not attached to a DockerClient", request.getId());
            Preconditions.checkArgument(jobContainers.containsKey(request.getId()), "Job ID %s does not have a known container ID", request.getId());

            DockerClient dockerClient = jobClients.get(request.getId());
            String containerId = jobContainers.get(request.getId());

            LOG.info("Stop requested for job {} running in container {}", request.getId(), containerId);

            try {
                stopContainer(dockerClient, containerId);

                responseObserver.onNext(StopContainerResponse.newBuilder().build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Failed to stop job: {}", request.getId(), e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            } finally {
                removeContainer(dockerClient, containerId);
                cleanUpJob(request.getId());
            }
        }
    }

    @Override
    public void waitForContainerExit(ExitParams request, StreamObserver<ContainerExitCode> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            Preconditions.checkArgument(jobClients.containsKey(request.getJob().getId()), "Job ID %s is not attached to a DockerClient", request.getJob().getId());
            Preconditions.checkArgument(jobContainers.containsKey(request.getJob().getId()), "Job ID %s does not have a known container ID", request.getJob().getId());

            DockerClient dockerClient = jobClients.get(request.getJob().getId());
            String containerId = jobContainers.get(request.getJob().getId());
            try {
                int exitCode = waitForContainer(dockerClient, containerId).awaitStatusCode();
                LOG.info("Received exit code from container {}: {}", containerId, exitCode);
                responseObserver.onNext(ContainerExitCode.newBuilder().setExitCode(exitCode).build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Failed to properly wait for container exit: {}", containerId, e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            } finally {
                removeContainer(dockerClient, containerId);
                cleanUpJob(request.getJob().getId());
            }
        }
    }

    @Override
    public void waitForContainerExitWithTimeout(ExitWithTimeoutParams request, StreamObserver<ContainerExitCode> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            Preconditions.checkArgument(jobClients.containsKey(request.getJob().getId()), "Job ID %s is not attached to a DockerClient", request.getJob().getId());
            Preconditions.checkArgument(jobContainers.containsKey(request.getJob().getId()), "Job ID %s does not have a known container ID", request.getJob().getId());

            DockerClient dockerClient = jobClients.get(request.getJob().getId());
            String containerId = jobContainers.get(request.getJob().getId());
            try {
                int exitCode = waitForContainer(dockerClient, containerId).awaitStatusCode(request.getTimeout(), TimeUnit.MINUTES);
                responseObserver.onNext(ContainerExitCode.newBuilder().setExitCode(exitCode).build());
                responseObserver.onCompleted();
            } catch (Exception e) {
                if (e.getClass().equals(DockerClientException.class) && e.getMessage().equals("Awaiting status code timeout.")) {
                    LOG.warn("Timed out waiting for application to exit; manually stopping container and treating as 'normal' exit: {}", containerId);
                    stopContainer(dockerClient, containerId);
                    responseObserver.onNext(ContainerExitCode.newBuilder().setExitCode(0).build());
                    responseObserver.onCompleted();
                } else {
                    LOG.error("Failed to properly wait for container exit: {}", containerId, e);
                    responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
                }
            } finally {
                removeContainer(dockerClient, containerId);
                cleanUpJob(request.getJob().getId());
            }
        }
    }

    @Override
    public void listOutputFiles(ListOutputFilesParam request, StreamObserver<OutputFileList> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            Path outputDir = Paths.get(request.getOutputsRootPath());
            LOG.debug("Listing outputs from job {} in path: {}", request.getJob().getId(), outputDir);

            OutputFileList.Builder responseBuilder = OutputFileList.newBuilder();

            try (Stream<Path> outputDirContents = Files.walk(outputDir, 3, FileVisitOption.FOLLOW_LINKS)) {
                outputDirContents.filter(Files::isRegularFile)
                        .map(Unchecked.function(outputDir::relativize))
                        .map(relativePath -> OutputFileItem.newBuilder().setRelativePath(relativePath.toString()).build())
                        .forEach(responseBuilder::addItems);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to list output files: {}", request.toString(), e);
            responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
        }
    }

    @Override
    public void getOutputFile(GetOutputFileParam request, StreamObserver<FileStream> responseObserver) {
        try (CloseableThreadContext.Instance ctc = getJobLoggingContext(request.getJob())) {
            try {
                Path outputFile = Paths.get(request.getPath());
                long outputFileSize = Files.size(outputFile);
                LOG.info("Returning output file from job {}: {} ({} bytes)", request.getJob().getId(), outputFile, outputFileSize);

                Stopwatch stopwatch = Stopwatch.createStarted();
                try (ReadableByteChannel channel = Files.newByteChannel(outputFile, StandardOpenOption.READ)) {
                    GrpcUtil.streamFile(
                            responseObserver,
                            outputFile.getFileName().toString(),
                            outputFileSize,
                            channel
                    );
                }
                LOG.info("Transferred output file {} ({} bytes) in {}", outputFile.getFileName(), outputFileSize, stopwatch.stop().elapsed());

                responseObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Failed to collect output file: {}", request.toString(), e);
                responseObserver.onError(new StatusRuntimeException(Status.fromCode(Status.Code.ABORTED).withCause(e)));
            }
        }
    }

    private void cleanUpJob(String jobId) {
        jobContainers.remove(jobId);
        jobClients.remove(jobId);
        nodeManager.releaseJobNode(jobId);
        Set<URI> finishedJobInputs = ImmutableSet.copyOf(jobInputs.removeAll(jobId));
        LOG.debug("Finished job URIs: {}", finishedJobInputs);
        Set<URI> unusedUris = Sets.difference(finishedJobInputs, ImmutableSet.copyOf(jobInputs.values()));
        LOG.debug("Unused URIs to be cleaned: {}", unusedUris);
        inputOutputManager.cleanUp(unusedUris);
    }

    private WaitContainerResultCallback waitForContainer(DockerClient dockerClient, String containerId) {
        return dockerClient.waitContainerCmd(containerId).exec(new WaitContainerResultCallback());
    }

    private void stopContainer(DockerClient client, String containerId) {
        if (client.inspectContainerCmd(containerId).exec().getState().getRunning()) {
            try {
                client.stopContainerCmd(containerId).withTimeout(30).exec();
                if (client.inspectContainerCmd(containerId).exec().getState().getRunning()) {
                    LOG.warn("Reached timeout trying to stop container safely; killing: {}", containerId);
                    client.killContainerCmd(containerId).exec();
                }
            } catch (DockerClientException e) {
                LOG.warn("Received exception trying to stop container; killing: {}", containerId, e);
                client.killContainerCmd(containerId).exec();
            }
        } else {
            LOG.debug("Container {} appears to already be stopped", containerId);
        }
    }

    private void removeContainer(DockerClient client, String containerId) {
        try {
            LOG.info("Removing container {}", containerId);
            client.removeContainerCmd(containerId).exec();
        } catch (Exception e) {
            LOG.error("Failed to delete container {}", containerId, e);
        }
    }

    private boolean isValidUri(String test) {
        try {
            URI uri = URI.create(test);
            return uri.getScheme() != null && inputOutputManager.isSupportedProtocol(uri.getScheme());
        } catch (Exception unused) {
            return false;
        }
    }

    private void buildDockerImage(DockerClient dockerClient, String serviceName, String dockerImage) throws IOException {
        try {
            // Retrieve service context files
            Path serviceContext = inputOutputManager.getServiceContext(serviceName);

            if (serviceContext == null || Files.list(serviceContext).count() == 0) {
                // If no service context files are available, shortcut and fall back on the hopefully-existent image tag
                LOG.warn("No service context files found for service '{}'; falling back on image tag", serviceName);
                return;
            } else if (!Files.exists(serviceContext.resolve("Dockerfile"))) {
                LOG.warn("Service context files exist, but no Dockerfile found for service '{}'; falling back on image tag", serviceName);
                return;
            }

            // Build image
            LOG.info("Building Docker image '{}' for service {}", dockerImage, serviceName);
            BuildImageCmd buildImageCmd = dockerClient.buildImageCmd()
                    .withRemove(true)
                    .withBaseDirectory(serviceContext.toFile())
                    .withDockerfile(serviceContext.resolve("Dockerfile").toFile())
                    .withTags(ImmutableSet.of(dockerImage));

            // Add proxy vars to the container, if they are set in the environment
            ImmutableSet.of("http_proxy", "https_proxy", "no_proxy").stream()
                    .filter(var -> System.getenv().containsKey(var))
                    .forEach(var -> buildImageCmd.withBuildArg(var, System.getenv(var)));

            String imageId = buildImageCmd.exec(new BuildImageResultCallback()).awaitImageId();

            // Tag image with desired image name
            LOG.debug("Tagged docker image {} with tag '{}'", imageId, dockerImage);
        } catch (ServiceIoException e) {
            LOG.error("Failed to retrieve Docker context files for service {}", serviceName, e);
            throw e;
        } catch (IOException e) {
            LOG.error("Failed to build Docker context for service {}", serviceName, e);
            throw e;
        }
    }
    
    private void pushDockerImage(DockerClient dockerClient, String dockerImage) throws IOException, InterruptedException {
        LOG.info("Pushing Docker image '{}' to registry {}", dockerImage, dockerRegistryConfig.getDockerRegistryUrl());
        PushImageCmd pushImageCmd = dockerClient.pushImageCmd(dockerImage);
        AuthConfig authConfig = new AuthConfig()
                .withRegistryAddress(dockerRegistryConfig.getDockerRegistryUrl())
                .withUsername(dockerRegistryConfig.getDockerRegistryUsername())
                .withPassword(dockerRegistryConfig.getDockerRegistryPassword());
        dockerClient.authCmd().withAuthConfig(authConfig).exec();
        pushImageCmd = pushImageCmd.withAuthConfig(authConfig);
        pushImageCmd.exec(new PushImageResultCallback()).awaitSuccess();
        LOG.info("Pushed Docker image '{}' to registry {}", dockerImage, dockerRegistryConfig.getDockerRegistryUrl());
    }
    
    private void pullDockerImage(DockerClient dockerClient, String dockerImage) throws IOException, InterruptedException {
        LOG.info("Pulling Docker image '{}' from registry {}", dockerImage, dockerRegistryConfig.getDockerRegistryUrl());
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(dockerImage);
        AuthConfig authConfig = new AuthConfig()
            .withRegistryAddress(dockerRegistryConfig.getDockerRegistryUrl())
            .withUsername(dockerRegistryConfig.getDockerRegistryUsername())
            .withPassword(dockerRegistryConfig.getDockerRegistryPassword());
        dockerClient.authCmd().withAuthConfig(authConfig).exec();
        pullImageCmd = pullImageCmd.withRegistry(dockerRegistryConfig.getDockerRegistryUrl()).withAuthConfig(authConfig);
        pullImageCmd.exec(new PullImageResultCallback()).awaitSuccess();
        LOG.info("Pulled Docker image '{}' from registry {}", dockerImage, dockerRegistryConfig.getDockerRegistryUrl());
    }
    
    private void removeDockerImage(DockerClient dockerClient, String dockerImageTag) {
        List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(dockerImageTag).exec();
        for (Image image: images) {
            dockerClient.removeImageCmd(image.getId()).exec();
        }
    }
    
    private boolean isImageAvailableLocally(DockerClient dockerClient, String dockerImage) {
        List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(dockerImage).exec();
        if (images.isEmpty()) {
            return false;
        }
        return true;
    }

    @VisibleForTesting
    Map<String, DockerClient> getJobClients() {
        return jobClients;
    }

    @VisibleForTesting
    Map<String, String> getJobContainers() {
        return jobContainers;
    }

    private static CloseableThreadContext.Instance getJobLoggingContext(Job job) {
        return CloseableThreadContext.push("FS-TEP Worker")
                .put("zooId", job.getId())
                .put("jobId", job.getIntJobId())
                .put("userId", job.getUserId())
                .put("serviceId", job.getServiceId());
    }

}
