package com.cgi.eoss.fstep.wps;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jooq.lambda.Seq;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.clouds.local.LocalNodeFactory;
import com.cgi.eoss.fstep.clouds.service.NodeFactory;
import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.io.ServiceInputOutputManager;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.Wallet;
import com.cgi.eoss.fstep.model.internal.OutputProductMetadata;
import com.cgi.eoss.fstep.orchestrator.service.FstepGuiServiceManager;
import com.cgi.eoss.fstep.orchestrator.service.FstepServiceLauncher;
import com.cgi.eoss.fstep.orchestrator.service.WorkerFactory;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.worker.worker.FstepWorker;
import com.cgi.eoss.fstep.worker.worker.FstepWorkerNodeManager;
import com.cgi.eoss.fstep.worker.worker.JobEnvironmentService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.io.MoreFiles;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import shadow.dockerjava.com.github.dockerjava.api.DockerClient;
import shadow.dockerjava.com.github.dockerjava.core.DefaultDockerClientConfig;
import shadow.dockerjava.com.github.dockerjava.core.DockerClientBuilder;
import shadow.dockerjava.com.github.dockerjava.core.DockerClientConfig;
import shadow.dockerjava.com.github.dockerjava.core.RemoteApiVersion;
import shadow.dockerjava.com.github.dockerjava.core.command.PullImageResultCallback;

/**
 * <p>Integration test for launching WPS services.</p> <p><strong>This uses a real Docker engine to build and run a
 * container!</strong></p>
 */
public class FstepServicesClientIT {
    private static final String RPC_SERVER_NAME = FstepServicesClientIT.class.getName();
    private static final String SERVICE_NAME = "service1";
    private static final String PARALLEL_SERVICE_NAME = "service2";
    private static final String TEST_CONTAINER_IMAGE = "alpine:latest";

    @Mock
    private FstepGuiServiceManager guiService;

    @Mock
    private JobDataService jobDataService;

    @Mock
    private CatalogueService catalogueService;

    @Mock
    private CostingService costingService;

    private Path workspace;
    private Path dataDir;
    private Path ingestedOutputsDir;

    private FstepServicesClient fstepServicesClient;

    private Server server;

    @BeforeClass
    public static void precondition() {
        // Shortcut if docker socket is not accessible to the current user
        assumeTrue("Unable to write to Docker socket; disabling docker tests", Files.isWritable(Paths.get("/var/run/docker.sock")));
        // TODO Pass in a DOCKER_HOST env var to allow remote docker engine use
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        workspace = Files.createTempDirectory(Paths.get("target"), FstepServicesClientIT.class.getSimpleName());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                MoreFiles.deleteRecursively(workspace);
            } catch (IOException ignored) {
            }
        }));
        ingestedOutputsDir = workspace.resolve("ingestedOutputsDir");
        Files.createDirectories(ingestedOutputsDir);
        dataDir = workspace.resolve("dataDir");
        Files.createDirectories(dataDir);
        
        when(catalogueService.provisionNewOutputProduct(any(), any())).thenAnswer(invocation -> {
            Path outputPath = ingestedOutputsDir.resolve(((OutputProductMetadata) invocation.getArgument(0)).getJobId()).resolve((String) invocation.getArgument(1));
            Files.createDirectories(outputPath.getParent());
            return outputPath;
        });
        when(catalogueService.ingestOutputProduct(any(), any())).thenAnswer(invocation -> {
            OutputProductMetadata outputProductMetadata = (OutputProductMetadata) invocation.getArgument(1);
            Path outputPath = (Path) invocation.getArgument(2);
            FstepFile fstepFile = new FstepFile(URI.create("fstep://outputs/" + ingestedOutputsDir.relativize(outputPath)), UUID.randomUUID());
            fstepFile.setFilename(ingestedOutputsDir.relativize(outputPath).toString());
            return fstepFile;
        });

        JobEnvironmentService jobEnvironmentService = spy(new JobEnvironmentService(workspace));
        ServiceInputOutputManager ioManager = mock(ServiceInputOutputManager.class);
        Mockito.when(ioManager.getServiceContext(SERVICE_NAME)).thenReturn(Paths.get("src/test/resources/service1").toAbsolutePath());

        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withApiVersion(RemoteApiVersion.VERSION_1_19)
                .withDockerHost("unix:///var/run/docker.sock")
                .build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build();
        NodeFactory nodeFactory = new LocalNodeFactory(-1, "unix:///var/run/docker.sock");
        FstepWorkerNodeManager nodeManager = new FstepWorkerNodeManager(nodeFactory, dataDir, Integer.MAX_VALUE);
        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(RPC_SERVER_NAME).directExecutor();
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(RPC_SERVER_NAME).directExecutor();

        WorkerFactory workerFactory = mock(WorkerFactory.class);
        FstepSecurityService securityService = mock(FstepSecurityService.class);

        FstepServiceLauncher fstepServiceLauncher = new FstepServiceLauncher(workerFactory, jobDataService, guiService, catalogueService, costingService, securityService);
        FstepWorker fstepWorker = new FstepWorker(nodeManager, jobEnvironmentService, ioManager, 0);

        when(workerFactory.getWorker(any())).thenReturn(FstepWorkerGrpc.newBlockingStub(channelBuilder.build()));

        inProcessServerBuilder.addService(fstepServiceLauncher);
        inProcessServerBuilder.addService(fstepWorker);

        server = inProcessServerBuilder.build().start();

        fstepServicesClient = new FstepServicesClient(channelBuilder);

        // Ensure the test image is available before testing
        dockerClient.pullImageCmd(TEST_CONTAINER_IMAGE).exec(new PullImageResultCallback()).awaitSuccess();
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void launchApplication() throws Exception {
        FstepService service = mock(FstepService.class);
        FstepServiceDescriptor serviceDescriptor = mock(FstepServiceDescriptor.class);
        User user = mock(User.class);
        when(user.getName()).thenReturn("fstep-user");
        Wallet wallet = mock(Wallet.class);
        when(user.getWallet()).thenReturn(wallet);
        when(wallet.getBalance()).thenReturn(100);

        when(service.getName()).thenReturn(SERVICE_NAME);
        when(service.getDockerTag()).thenReturn("fstep/testservice1");
        when(service.getType()).thenReturn(FstepService.Type.APPLICATION); // Trigger ingestion of all outputs
        when(service.getServiceDescriptor()).thenReturn(serviceDescriptor);
        List<Job> launchedJobs = new ArrayList<>();
        when(jobDataService.buildNew(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            JobConfig config = new JobConfig(user, service);
            config.setLabel(Strings.isNullOrEmpty(invocation.getArgument(3)) ? null : invocation.getArgument(3));
            config.setInputs(invocation.getArgument(4));
            Job job = new Job(config, invocation.getArgument(0), user);
            job.setId(1L);
            launchedJobs.add(job);
            return job;
        });

        String jobId = UUID.randomUUID().toString();
        String userId = "userId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("input", "inputVal1")
                .putAll("inputKey2", ImmutableList.of("inputVal2-1", "inputVal2-2"))
                .build();

        when(costingService.estimateJobCost(any())).thenReturn(20);

        Multimap<String, String> outputs = fstepServicesClient.launchService(userId, SERVICE_NAME, jobId, inputs);

        assertThat(launchedJobs.size(), is(1));

        assertThat(outputs, is(notNullValue()));
        assertThat(outputs.get("1"), containsInAnyOrder("fstep://outputs/" + launchedJobs.get(0).getExtId() + "/output_file_1"));

        List<String> jobConfigLines = Files.readAllLines(workspace.resolve("Job_" + jobId + "/FSTEP-WPS-INPUT.properties"));
        assertThat(jobConfigLines, is(ImmutableList.of(
                "input=\"inputVal1\"",
                "inputKey2=\"inputVal2-1,inputVal2-2\""
        )));

        List<String> outputFileLines = Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(0).getExtId()).resolve("output_file_1"));
        assertThat(outputFileLines, is(ImmutableList.of("INPUT PARAM: inputVal1")));

        verify(costingService).chargeForJob(eq(wallet), any());
    }

    @Test
    public void launchProcessor() throws Exception {
        FstepService service = mock(FstepService.class);
        FstepServiceDescriptor serviceDescriptor = mock(FstepServiceDescriptor.class);
        User user = mock(User.class);
        when(user.getName()).thenReturn("fstep-user");
        Wallet wallet = mock(Wallet.class);
        when(user.getWallet()).thenReturn(wallet);
        when(wallet.getBalance()).thenReturn(100);

        when(service.getName()).thenReturn(SERVICE_NAME);
        when(service.getDockerTag()).thenReturn("fstep/testservice1");
        when(service.getType()).thenReturn(FstepService.Type.PROCESSOR);
        when(service.getServiceDescriptor()).thenReturn(serviceDescriptor);
        when(serviceDescriptor.getDataOutputs()).thenReturn(ImmutableList.of(
                FstepServiceDescriptor.Parameter.builder().id("output").build()
        ));
        List<Job> launchedJobs = new ArrayList<>();
        when(jobDataService.buildNew(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            JobConfig config = new JobConfig(user, service);
            config.setLabel(Strings.isNullOrEmpty(invocation.getArgument(3)) ? null : invocation.getArgument(3));
            config.setInputs(invocation.getArgument(4));
            Job job = new Job(config, invocation.getArgument(0), user);
            job.setId(1L);
            launchedJobs.add(job);
            return job;
        });

        String jobId = UUID.randomUUID().toString();
        String userId = "userId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("input", "inputVal1")
                .putAll("inputKey2", ImmutableList.of("inputVal2-1", "inputVal2-2"))
                .build();

        when(costingService.estimateJobCost(any())).thenReturn(20);

        Multimap<String, String> outputs = fstepServicesClient.launchService(userId, SERVICE_NAME, jobId, inputs);

        assertThat(launchedJobs.size(), is(1));

        assertThat(outputs, is(notNullValue()));
        assertThat(outputs.get("output"), containsInAnyOrder("fstep://outputs/" + launchedJobs.get(0).getExtId() + "/output_file_1"));

        List<String> jobConfigLines = Files.readAllLines(workspace.resolve("Job_" + jobId + "/FSTEP-WPS-INPUT.properties"));
        assertThat(jobConfigLines, is(ImmutableList.of(
                "input=\"inputVal1\"",
                "inputKey2=\"inputVal2-1,inputVal2-2\""
        )));

        List<String> outputFileLines = Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(0).getExtId()).resolve("output_file_1"));
        assertThat(outputFileLines, is(ImmutableList.of("INPUT PARAM: inputVal1")));

        verify(costingService).chargeForJob(eq(wallet), any());
    }

    @Test
    public void launchParallelProcessor() throws Exception {
        FstepService service = mock(FstepService.class);
        FstepServiceDescriptor serviceDescriptor = mock(FstepServiceDescriptor.class);
        User user = mock(User.class);
        when(user.getName()).thenReturn("fstep-user");
        Wallet wallet = mock(Wallet.class);
        when(user.getWallet()).thenReturn(wallet);
        when(wallet.getBalance()).thenReturn(100);

        when(service.getName()).thenReturn(PARALLEL_SERVICE_NAME);
        when(service.getDockerTag()).thenReturn("fstep/testservice1");
        when(service.getType()).thenReturn(FstepService.Type.PARALLEL_PROCESSOR);
        when(service.getServiceDescriptor()).thenReturn(serviceDescriptor);
        when(serviceDescriptor.getDataOutputs()).thenReturn(ImmutableList.of(
                FstepServiceDescriptor.Parameter.builder().id("output").build()
        ));

        final long[] jobCount = {0L};
        List<Job> launchedJobs = new ArrayList<>();

        when(jobDataService.buildNew(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            JobConfig config = new JobConfig(user, service);
            config.setLabel(Strings.isNullOrEmpty(invocation.getArgument(3)) ? null : invocation.getArgument(3));
            config.setInputs(invocation.getArgument(4));
            Job job = new Job(config, invocation.getArgument(0), user);
            job.setId(jobCount[0]++);
            launchedJobs.add(job);
            return job;
        });

        String jobId = UUID.randomUUID().toString();
        String userId = "userId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("parallelInputs", "parallelInput1,parallelInput2,parallelInput3")
                .put("sharedInputFoo", "foo")
                .put("sharedInputBarBaz", "bar,baz")
                .build();

        when(costingService.estimateJobCost(any())).thenReturn(20);

        Multimap<String, String> outputs = fstepServicesClient.launchService(userId, PARALLEL_SERVICE_NAME, jobId, inputs);

        assertThat(launchedJobs.size(), is(4));

        assertThat(outputs, is(notNullValue()));
        assertThat(ImmutableSet.copyOf(outputs.get("output")), is(Seq.of(1, 2, 3).stream()
                .map(i -> {
                    Job job = launchedJobs.get(i);
                    return "fstep://outputs/" + job.getExtId() + "/output_file_1";
                })
                .collect(toSet())));

        assertThat(Files.exists(workspace.resolve("Job_" + launchedJobs.get(0).getExtId()).resolve("FSTEP-WPS-INPUT.properties")), is(false));
        assertThat(Files.readAllLines(workspace.resolve("Job_" + launchedJobs.get(1).getExtId()).resolve("FSTEP-WPS-INPUT.properties")), is(ImmutableList.of(
                "sharedInputFoo=\"foo\"",
                "sharedInputBarBaz=\"bar,baz\"",
                "input=\"parallelInput1\""
        )));
        assertThat(Files.readAllLines(workspace.resolve("Job_" + launchedJobs.get(2).getExtId()).resolve("FSTEP-WPS-INPUT.properties")), is(ImmutableList.of(
                "sharedInputFoo=\"foo\"",
                "sharedInputBarBaz=\"bar,baz\"",
                "input=\"parallelInput2\""
        )));
        assertThat(Files.readAllLines(workspace.resolve("Job_" + launchedJobs.get(3).getExtId()).resolve("FSTEP-WPS-INPUT.properties")), is(ImmutableList.of(
                "sharedInputFoo=\"foo\"",
                "sharedInputBarBaz=\"bar,baz\"",
                "input=\"parallelInput3\""
        )));

        assertThat(Files.exists(ingestedOutputsDir.resolve(launchedJobs.get(0).getExtId()).resolve("output_file_1")), is(false));
        assertThat(Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(1).getExtId()).resolve("output_file_1")), is(ImmutableList.of(
                "INPUT PARAM: parallelInput1"
        )));
        assertThat(Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(2).getExtId()).resolve("output_file_1")), is(ImmutableList.of(
                "INPUT PARAM: parallelInput2"
        )));
        assertThat(Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(3).getExtId()).resolve("output_file_1")), is(ImmutableList.of(
                "INPUT PARAM: parallelInput3"
        )));

        verify(costingService).chargeForJob(eq(wallet), any());
    }

}
