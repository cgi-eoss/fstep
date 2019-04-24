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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import com.cgi.eoss.fstep.model.CostQuotation;
import com.cgi.eoss.fstep.model.CostQuotation.Recurrence;
import com.cgi.eoss.fstep.costing.CostingService;
import com.cgi.eoss.fstep.io.PersistentFolderMounter;
import com.cgi.eoss.fstep.io.PersistentFolderProvider;
import com.cgi.eoss.fstep.io.ServiceInputOutputManager;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.JobProcessing;
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
import com.cgi.eoss.fstep.persistence.service.FstepFilesRelationDataService;
import com.cgi.eoss.fstep.persistence.service.JobDataService;
import com.cgi.eoss.fstep.persistence.service.JobProcessingDataService;
import com.cgi.eoss.fstep.persistence.service.PersistentFolderDataService;
import com.cgi.eoss.fstep.persistence.service.QuotaDataService;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
import com.cgi.eoss.fstep.persistence.service.UserMountDataService;
import com.cgi.eoss.fstep.persistence.service.WalletDataService;
import com.cgi.eoss.fstep.persistence.service.WalletTransactionDataService;
import com.cgi.eoss.fstep.queues.service.FstepJMSQueueService;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.rpc.LocalWorker;
import com.cgi.eoss.fstep.rpc.worker.Binding;
import com.cgi.eoss.fstep.rpc.worker.FstepWorkerGrpc;
import com.cgi.eoss.fstep.rpc.worker.PortBinding;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.worker.jobs.WorkerJobDataService;
import com.cgi.eoss.fstep.worker.jobs.WorkerJob;
import com.cgi.eoss.fstep.worker.worker.DockerEventsListener;
import com.cgi.eoss.fstep.worker.worker.FstepWorker;
import com.cgi.eoss.fstep.worker.worker.FstepWorkerDispatcher;
import com.cgi.eoss.fstep.worker.worker.FstepWorkerNodeManager;
import com.cgi.eoss.fstep.worker.worker.FstepWorkerUpdateManager;
import com.cgi.eoss.fstep.worker.worker.JobEnvironmentService;
import com.cgi.eoss.fstep.worker.worker.JobExecutionController;
import com.cgi.eoss.fstep.worker.worker.LocalEventCollectorNodePreparer;
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
import shadow.dockerjava.com.github.dockerjava.api.model.Event;
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
    
    @Mock
    private JobProcessingDataService jobProcessingDataService;
    
    @Mock
    private WalletDataService walletDataService;
    
    @Mock
    private WalletTransactionDataService walletTransactionDataService;
    
    @Mock
    private PersistentFolderDataService persistentFolderDataService;
    
    @Mock
    private FstepFilesRelationDataService filesRelationDataService;
    
    private Path workspace;
    private Path ingestedOutputsDir;

    private FstepServicesClient fstepServicesClient;

    private Server server;
	private Timer jobDispatcherTimer;
	private Timer jobUpdatesTimer;
	private Timer queueSchedulerTimer;
	private Timer dockerEventsTimer;
	
	
	private BrokerService broker;
    
	@Mock
    private WorkerJobDataService workerDataService;
	
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

        when(catalogueService.provisionNewOutputProduct(any(), any(), any())).thenAnswer(invocation -> {
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
        PersistentFolderProvider persistentFolderProvider = mock(PersistentFolderProvider.class);
        Mockito.when(persistentFolderProvider.userFolderExists(any())).thenReturn(false);

        PersistentFolderMounter persistentFolderMounter = mock(PersistentFolderMounter.class);
        Mockito.when(persistentFolderMounter.supportsPersistentFolder(any())).thenReturn(false);

        Mockito.when(ioManager.getServiceContext(SERVICE_NAME)).thenReturn(Paths.get("src/test/resources/service1").toAbsolutePath());

        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withApiVersion(RemoteApiVersion.VERSION_1_19)
                .withDockerHost("unix:///var/run/docker.sock")
                .build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build();
        
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
        connector.setUri(new URI("tcp://0.0.0.0:61616"));
        broker.addConnector(connector);
        broker.start();
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://0.0.0.0:61616");
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
        		catalogueService, securityService, jobProcessingDataService, costingService, walletDataService, walletTransactionDataService, filesRelationDataService);
        String workerId = "local1";
        JobValidator jobValidator = new JobValidator(costingService, catalogueService);
        FstepJobLauncher fstepJobLauncher = new FstepJobLauncher(workerFactory, jobDataService, jobProcessingDataService, databasketDataService, guiService, 
        		 costingService, securityService, queueService, userMountDataService, serviceDataService, dynamicProxyService, persistentFolderDataService, jobValidator, updatesManager);
        LocalEventCollectorNodePreparer nodePreparer = new LocalEventCollectorNodePreparer(blockingJmsTemplate, DockerEventsListener.DOCKER_EVENTS_QUEUE);
		FstepWorkerNodeManager nodeManager = new FstepWorkerNodeManager(new LocalNodeFactory(-1, "unix:///var/run/docker.sock"), workspace.resolve("dl"), 2, workerDataService, nodePreparer);

        FstepWorker fstepWorker = new FstepWorker(nodeManager, jobEnvironmentService, ioManager, 1, workerDataService, persistentFolderProvider, persistentFolderMounter);
        fstepWorker.allocateMinNodes();
        FstepWorkerUpdateManager fstepWorkerUpdateManager = new FstepWorkerUpdateManager(queueService, workerId);
        JobExecutionController jobExecutionController = new JobExecutionController(new LocalWorker(channelBuilder), nodeManager, workerDataService, fstepWorkerUpdateManager);
        FstepWorkerDispatcher fstepWorkerDispatcher = new FstepWorkerDispatcher(queueService, jobExecutionController);
        
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
        dockerEventsTimer = new Timer();
        dockerEventsTimer.schedule(new TimerTask() {
            
            @Override
            public void run() {
            	if (broker.isStopped() || broker.isStopping()) {
            		return;
            	}
            	com.cgi.eoss.fstep.queues.service.Message message =  queueService.receiveNoWait(DockerEventsListener.DOCKER_EVENTS_QUEUE);
            	while (message != null) {
            		fstepWorkerDispatcher.receiveDockerEvent((Event) message.getPayload());
            		message =  queueService.receiveNoWait(DockerEventsListener.DOCKER_EVENTS_QUEUE);
            	}
            }
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
        List<JobProcessing> launchedJobProcessings = new ArrayList<>();
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
        
        when(jobProcessingDataService.buildNew(any(Job.class))).thenAnswer(invocation -> {
        	JobProcessing jobProcessing = new JobProcessing(invocation.getArgument(0), 1L);
        	launchedJobProcessings.add(jobProcessing);
            return jobProcessing;
        });
        
        when(jobProcessingDataService.findByJobAndMaxSequenceNum(any(Job.class))).thenAnswer(invocation -> {
            return launchedJobProcessings.stream().filter(jp -> jp.getJob().getId() == ((Job)invocation.getArgument(0)).getId()).findFirst().get();
        });
        
        when (guiService.getGuiPortBinding(any(), any())).thenReturn(PortBinding.newBuilder().setBinding(Binding.newBuilder().setIp("test").setPort(8080).build()).build());
        when(dynamicProxyService.getProxyEntry(any(),any(), anyInt())).thenReturn(new ReverseProxyEntry("test", "test"));
        
        Set<WorkerJob> workerJobs = new HashSet<>();
        when(workerDataService.save(any())).thenAnswer(invocation -> {
        	WorkerJob workerJob = invocation.getArgument(0);
        	if (workerJobs.contains(workerJob)) {
        		workerJobs.remove(workerJob);
        	}
        	workerJobs.add(workerJob);
            return workerJob;
        });
        
        when(workerDataService.assignJobToNode(anyInt(), any(), any())).thenAnswer(invocation -> {
        	WorkerJob workerJob = invocation.getArgument(1);
        	workerJob.setWorkerNodeId(invocation.getArgument(2));
        	workerJobs.add(workerJob);
            return true;
        });
        
        when(workerDataService.findByJobId(any())).thenAnswer(invocation -> {
        	Optional<WorkerJob> workerJob = workerJobs.stream().filter(j -> j.getJobId().equals(invocation.getArgument(0))).findFirst();
        	if (workerJob.isPresent()) {
        		return workerJob.get();
        	}
        	return null;
        });
        
        String jobId = UUID.randomUUID().toString();
        String userId = "userId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("input", "inputVal1")
                .putAll("inputKey2", ImmutableList.of("inputVal2-1", "inputVal2-2"))
                .build();
        
        when(costingService.estimateJobCost(any())).thenReturn(new CostQuotation(20, Recurrence.ONE_OFF));
        
        when(costingService.estimateSingleRunJobCost(any())).thenReturn(new CostQuotation(20, Recurrence.ONE_OFF));

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

        verify(costingService).chargeForJobProcessing(eq(wallet), any());
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
        List<JobProcessing> launchedJobProcessings = new ArrayList<>();
        

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
        
        when(jobProcessingDataService.buildNew(any(Job.class))).thenAnswer(invocation -> {
        	JobProcessing jobProcessing = new JobProcessing(invocation.getArgument(0), 1L);
        	launchedJobProcessings.add(jobProcessing);
            return jobProcessing;
        });
        
        when(jobProcessingDataService.findByJobAndMaxSequenceNum(any(Job.class))).thenAnswer(invocation -> {
            return launchedJobProcessings.stream().filter(jp -> jp.getJob().getId() == ((Job)invocation.getArgument(0)).getId()).findFirst().get();
        });
        
        Set<WorkerJob> workerJobs = new HashSet<>();
        when(workerDataService.save(any())).thenAnswer(invocation -> {
        	WorkerJob workerJob = invocation.getArgument(0);
        	if (workerJobs.contains(workerJob)) {
        		workerJobs.remove(workerJob);
        	}
        	workerJobs.add(workerJob);
            return workerJob;
        });
        
        when(workerDataService.assignJobToNode(anyInt(), any(), any())).thenAnswer(invocation -> {
        	WorkerJob workerJob = invocation.getArgument(1);
        	workerJob.setWorkerNodeId(invocation.getArgument(2));
        	workerJobs.add(workerJob);
            return true;
        });
        
        when(workerDataService.findByJobId(any())).thenAnswer(invocation -> {
        	Optional<WorkerJob> workerJob = workerJobs.stream().filter(j -> j.getJobId().equals(invocation.getArgument(0))).findFirst();
        	if (workerJob.isPresent()) {
        		return workerJob.get();
        	}
        	return null;
        });
        String jobId = UUID.randomUUID().toString();
        String userId = "userId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("input", "inputVal1")
                .putAll("inputKey2", ImmutableList.of("inputVal2-1", "inputVal2-2"))
                .build();

        when(costingService.estimateJobCost(any())).thenReturn(new CostQuotation(20, Recurrence.ONE_OFF));
        when(costingService.estimateSingleRunJobCost(any())).thenReturn(new CostQuotation(20, Recurrence.ONE_OFF));

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

        verify(costingService).chargeForJobProcessing(eq(wallet), any());
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
        List<JobProcessing> launchedJobProcessings = new ArrayList<>();
       
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
        
        when(jobDataService.save(any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
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
        
        when(jobProcessingDataService.buildNew(any(Job.class))).thenAnswer(invocation -> {
        	JobProcessing jobProcessing = new JobProcessing(invocation.getArgument(0), 1L);
        	launchedJobProcessings.add(jobProcessing);
            return jobProcessing;
        });
        
        when(jobProcessingDataService.findByJobAndMaxSequenceNum(any(Job.class))).thenAnswer(invocation -> {
            return launchedJobProcessings.stream().filter(jp -> jp.getJob().getId() == ((Job)invocation.getArgument(0)).getId()).findFirst().get();
        });
        
        Set<WorkerJob> workerJobs = new HashSet<>();
        when(workerDataService.save(any())).thenAnswer(invocation -> {
        	WorkerJob workerJob = invocation.getArgument(0);
        	if (workerJobs.contains(workerJob)) {
        		workerJobs.remove(workerJob);
        	}
        	workerJobs.add(workerJob);
            return workerJob;
        });
        
        when(workerDataService.assignJobToNode(anyInt(), any(), any())).thenAnswer(invocation -> {
        	WorkerJob workerJob = invocation.getArgument(1);
        	workerJob.setWorkerNodeId(invocation.getArgument(2));
        	workerJobs.add(workerJob);
            return true;
        });
        
        when(workerDataService.findByJobId(any())).thenAnswer(invocation -> {
        	Optional<WorkerJob> workerJob = workerJobs.stream().filter(j -> j.getJobId().equals(invocation.getArgument(0))).findFirst();
        	if (workerJob.isPresent()) {
        		return workerJob.get();
        	}
        	return null;
        });
       
        String jobId = UUID.randomUUID().toString();
        String userId = "userId";
        Multimap<String, String> inputs = ImmutableMultimap.<String, String>builder()
                .put("parallelInputs", "parallelInput1,parallelInput2,parallelInput3")
                .put("sharedInputFoo", "foo")
                .put("sharedInputBarBaz", "bar,baz")
                .build();

        when(costingService.estimateJobCost(any())).thenReturn(new CostQuotation(20, Recurrence.ONE_OFF));
        when(costingService.estimateSingleRunJobCost(any())).thenReturn(new CostQuotation(20, Recurrence.ONE_OFF));

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

        verify(costingService, times(3)).chargeForJobProcessing(eq(wallet), any());
    }

}
