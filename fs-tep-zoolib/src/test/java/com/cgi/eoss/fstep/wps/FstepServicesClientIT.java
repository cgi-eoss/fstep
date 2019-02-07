package com.cgi.eoss.fstep.wps;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.jooq.lambda.Seq;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.jms.core.JmsTemplate;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.clouds.local.LocalNodeFactory;
import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.io.ServiceInputOutputManager;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.Wallet;
import com.cgi.eoss.fstep.model.internal.OutputProductMetadata;
import com.cgi.eoss.fstep.orchestrator.service.CachingWorkerFactory;
import com.cgi.eoss.fstep.orchestrator.service.DynamicProxyService;
import com.cgi.eoss.fstep.orchestrator.service.FstepGuiServiceManager;
import com.cgi.eoss.fstep.orchestrator.service.FstepJobLauncher;
import com.cgi.eoss.fstep.orchestrator.service.FstepJobUpdatesManager;
import com.cgi.eoss.fstep.orchestrator.service.JobValidator;
import com.cgi.eoss.fstep.orchestrator.service.QueueScheduler;
import com.cgi.eoss.fstep.orchestrator.service.ReverseProxyEntry;
import com.cgi.eoss.fstep.persistence.service.DatabasketDataService;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.persistence.service.QuotaDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
import com.cgi.eoss.fstep.persistence.service.UserMountDataService;
import com.cgi.eoss.fstep.queues.service.FstepJMSQueueService;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.rpc.LocalWorker;
import com.cgi.eoss.fstep.rpc.worker.Binding;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.rpc.worker.PortBinding;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.worker.worker.FstepWorker;
import com.cgi.eoss.fstep.worker.worker.FstepWorkerDispatcher;
import com.cgi.eoss.fstep.worker.worker.FstepWorkerNodeManager;
import com.cgi.eoss.fstep.worker.worker.JobEnvironmentService;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
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
    
    @Mock
    private DynamicProxyService dynamicProxyService;

    private Path workspace;
    private Path ingestedOutputsDir;

    private FstepServicesClient fstepServicesClient;

    private Server server;
	private Timer jobDispatcherTimer;
	private Timer jobUpdatesTimer;
	private Timer queueSchedulerTimer;
	
	private BrokerService broker;
    
    
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

        when(catalogueService.provisionNewOutputProduct(any(), any())).thenAnswer(invocation -> {
            Path outputPath = ingestedOutputsDir.resolve(((OutputProductMetadata) invocation.getArgument(0)).getJobId()).resolve((String) invocation.getArgument(1));
            Files.createDirectories(outputPath.getParent());
            return outputPath;
        });
        when(catalogueService.ingestOutputProduct(any(), any())).thenAnswer(invocation -> {
        	Path outputPath = (Path) invocation.getArgument(1);
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
        FstepWorkerNodeManager nodeManager = new FstepWorkerNodeManager(new LocalNodeFactory(-1, "unix:///var/run/docker.sock"), workspace.resolve("dl"), 2);

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(RPC_SERVER_NAME).directExecutor();
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(RPC_SERVER_NAME).directExecutor();

        CachingWorkerFactory workerFactory = mock(CachingWorkerFactory.class);
        FstepSecurityService securityService = mock(FstepSecurityService.class);
        DatabasketDataService databasketDataService = mock(DatabasketDataService.class);
        UserMountDataService userMountDataService = mock(UserMountDataService.class);
        ServiceDataService serviceDataService = mock(ServiceDataService.class);
        QuotaDataService quotaDataService = mock(QuotaDataService.class);
        broker = new BrokerService();
        broker.setBrokerName("broker1");
        broker.setUseJmx(false);
        broker.setPlugins(new BrokerPlugin[]{new StatisticsBrokerPlugin()});
        broker.setPersistent(false);
        PolicyMap pm = new PolicyMap();
        PolicyEntry pe = new PolicyEntry();
        pe.setPrioritizedMessages(true);
        pm.setDefaultEntry(pe);
        broker.setDestinationPolicy(pm);
        TransportConnector connector = new TransportConnector();
        connector.setUri(new URI("vm://broker1"));
        broker.addConnector(connector);
        broker.start();
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://broker1?create=false");
        connectionFactory.setTrustAllPackages(true);
        JmsTemplate nonblockingJmsTemplate = new JmsTemplate(new PooledConnectionFactory(connectionFactory)) {
	          @Override
	          protected void doSend(MessageProducer producer, Message message) throws JMSException {
	        	  producer.send(message, getDeliveryMode(), message.getJMSPriority(), getTimeToLive());
	          }
        };
        nonblockingJmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
        JmsTemplate blockingJmsTemplate = new JmsTemplate(new PooledConnectionFactory(connectionFactory)) {
	          @Override
	          protected void doSend(MessageProducer producer, Message message) throws JMSException {
	        	  producer.send(message, getDeliveryMode(), message.getJMSPriority(), getTimeToLive());
	          }
        };
        blockingJmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
        FstepQueueService queueService = new FstepJMSQueueService(blockingJmsTemplate, nonblockingJmsTemplate);
        when(workerFactory.getWorker(any())).thenReturn(FstepWorkerGrpc.newBlockingStub(channelBuilder.build()));
        when(workerFactory.getWorkerById(any())).thenReturn(FstepWorkerGrpc.newBlockingStub(channelBuilder.build()));
        FstepJobUpdatesManager updatesManager = new FstepJobUpdatesManager(jobDataService, dynamicProxyService, guiService, workerFactory, 
        		catalogueService, securityService);
        String workerId = "local1";
        JobValidator jobValidator = new JobValidator(costingService, catalogueService);
        FstepJobLauncher fstepJobLauncher = new FstepJobLauncher(workerFactory, jobDataService, databasketDataService, guiService, 
        		 costingService, securityService, queueService, userMountDataService, serviceDataService, dynamicProxyService, jobValidator, updatesManager);
        
        FstepWorker fstepWorker = new FstepWorker(nodeManager, jobEnvironmentService, ioManager, 1);
        fstepWorker.allocateMinNodes();
        FstepWorkerDispatcher fstepWorkerDispatcher = new FstepWorkerDispatcher(queueService, new LocalWorker(channelBuilder), workerId, nodeManager);
        
        QueueScheduler q = new QueueScheduler(jobDataService, queueService, quotaDataService);
        jobDispatcherTimer = new Timer();
        jobDispatcherTimer.schedule(new TimerTask() {
            
            @Override
            public void run() {
            	if (broker.isStopped() || broker.isStopping()) {
            		return;
            	}
            	fstepWorkerDispatcher.getNewJobs();
                
            }
        }, 0, 1000);
        
        jobUpdatesTimer = new Timer();
        jobUpdatesTimer.schedule(new TimerTask() {
            
            @Override
            public void run() {
            	if (broker.isStopped() || broker.isStopping()) {
            		return;
            	}
            	com.cgi.eoss.fstep.queues.service.Message message =  queueService.receiveNoWait(FstepQueueService.jobUpdatesQueueName);
            	while (message != null) {
            		updatesManager.receiveJobUpdate(message.getPayload(), (String) message.getHeaders().get("workerId"), (String) message.getHeaders().get("jobId"));
            		message =  queueService.receiveNoWait(FstepQueueService.jobUpdatesQueueName);
            	}
            }
        }, 0, 1000);
        
        queueSchedulerTimer = new Timer();
        queueSchedulerTimer.schedule(new TimerTask() {
            
            @Override
            public void run() {
            	if (broker.isStopped() || broker.isStopping()) {
            		return;
            	}
            	q.updateQueues();            }
        }, 0, 1000);
        inProcessServerBuilder.addService(fstepJobLauncher);
        inProcessServerBuilder.addService(fstepWorker);

        server = inProcessServerBuilder.build().start();

        fstepServicesClient = new FstepServicesClient(channelBuilder);

        // Ensure the test image is available before testing
        dockerClient.pullImageCmd(TEST_CONTAINER_IMAGE).exec(new PullImageResultCallback()).awaitSuccess();
    }

    @After
    public void tearDown() throws Exception {
        server.shutdownNow();
        jobDispatcherTimer.cancel();
        jobUpdatesTimer.cancel();
        broker.stop();
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
        
        when(jobDataService.getById(any())).thenAnswer(invocation -> {
            return launchedJobs.stream().filter(j -> j.getId() == invocation.getArgument(0)).findFirst().get();
        });
        
        when(jobDataService.refreshFull(anyLong())).thenAnswer(invocation -> {
            return launchedJobs.stream().filter(j -> j.getId() == invocation.getArgument(0)).findFirst().get();
        });
      
        when(jobDataService.refreshFull(any(Job.class))).thenAnswer(invocation -> {
            return launchedJobs.stream().filter(j -> j.getId() == ((Job)invocation.getArgument(0)).getId()).findFirst().get();
        });
        
        when (guiService.getGuiPortBinding(any(), any())).thenReturn(PortBinding.newBuilder().setBinding(Binding.newBuilder().setIp("test").setPort(8080).build()).build());
        when(dynamicProxyService.getProxyEntry(any(),any(), anyInt())).thenReturn(new ReverseProxyEntry("test", "test"));
        
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
        assertThat(outputs.get("1"), containsInAnyOrder("fstep://outputs/" + launchedJobs.get(0).getExtId() + "/output/output_file_1"));

        List<String> jobConfigLines = Files.readAllLines(workspace.resolve("Job_" + jobId + "/FSTEP-WPS-INPUT.properties"));
        assertThat(jobConfigLines, is(ImmutableList.of(
                "input=\"inputVal1\"",
                "inputKey2=\"inputVal2-1,inputVal2-2\""
        )));

        List<String> outputFileLines = Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(0).getExtId()).resolve("output/output_file_1"));
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
        
        when(jobDataService.getById(any())).thenAnswer(invocation -> {
            return launchedJobs.stream().filter(j -> j.getId() == invocation.getArgument(0)).findFirst().get();
        });
        
        when(jobDataService.refreshFull(anyLong())).thenAnswer(invocation -> {
            return launchedJobs.stream().filter(j -> j.getId() == invocation.getArgument(0)).findFirst().get();
        });
        
        when(jobDataService.refreshFull(any(Job.class))).thenAnswer(invocation -> {
            return launchedJobs.stream().filter(j -> j.getId() == ((Job)invocation.getArgument(0)).getId()).findFirst().get();
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
        assertThat(outputs.get("output"), containsInAnyOrder("fstep://outputs/" + launchedJobs.get(0).getExtId() + "/output/output_file_1"));

        List<String> jobConfigLines = Files.readAllLines(workspace.resolve("Job_" + jobId + "/FSTEP-WPS-INPUT.properties"));
        assertThat(jobConfigLines, is(ImmutableList.of(
                "input=\"inputVal1\"",
                "inputKey2=\"inputVal2-1,inputVal2-2\""
        )));

        List<String> outputFileLines = Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(0).getExtId()).resolve("output/output_file_1"));
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
        
        when(jobDataService.buildNew(any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            JobConfig config = new JobConfig(user, service);
            config.setLabel(Strings.isNullOrEmpty(invocation.getArgument(3)) ? null : invocation.getArgument(3));
            config.setInputs(invocation.getArgument(4));
            Job job = new Job(config, invocation.getArgument(0), user, invocation.getArgument(5));
            job.setId(jobCount[0]++);
            launchedJobs.add(job);
            return job;
        });
        
        when(jobDataService.getById(any())).thenAnswer(invocation -> {
            return launchedJobs.stream().filter(j -> j.getId() == invocation.getArgument(0)).findFirst().get();
        });
        
        when(jobDataService.refreshFull(anyLong())).thenAnswer(invocation -> {
            return launchedJobs.stream().filter(j -> j.getId() == invocation.getArgument(0)).findFirst().get();
        });
      
        when(jobDataService.refreshFull(any(Job.class))).thenAnswer(invocation -> {
            return launchedJobs.stream().filter(j -> j.getId() == ((Job)invocation.getArgument(0)).getId()).findFirst().get();
        });
      
        when(jobDataService.updateParentJob(any())).thenAnswer(invocation -> {
        	Job job = launchedJobs.stream().filter(j -> j.getId().equals(((Job) invocation.getArgument(0)).getId())).findFirst().get();
            Job parentJob = job.getParentJob();
    		if (parentJob.getOutputs() == null) {
    			parentJob.setOutputs(ArrayListMultimap.create());
    		} 
    		parentJob.getOutputs().putAll(job.getOutputs());
    		parentJob.getOutputFiles().addAll(ImmutableSet.copyOf(job.getOutputFiles()));
            return parentJob;
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
                    return "fstep://outputs/" + job.getExtId() + "/output/output_file_1";
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

        assertThat(Files.exists(ingestedOutputsDir.resolve(launchedJobs.get(0).getExtId()).resolve("output/output_file_1")), is(false));
        assertThat(Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(1).getExtId()).resolve("output/output_file_1")), is(ImmutableList.of(
                "INPUT PARAM: parallelInput1"
        )));
        assertThat(Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(2).getExtId()).resolve("output/output_file_1")), is(ImmutableList.of(
                "INPUT PARAM: parallelInput2"
        )));
        assertThat(Files.readAllLines(ingestedOutputsDir.resolve(launchedJobs.get(3).getExtId()).resolve("output/output_file_1")), is(ImmutableList.of(
                "INPUT PARAM: parallelInput3"
        )));

        verify(costingService, times(3)).chargeForJob(eq(wallet), any());
    }

}
