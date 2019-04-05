package com.cgi.eoss.fstep.worker;

import com.cgi.eoss.fstep.clouds.CloudsConfig;
import com.cgi.eoss.fstep.clouds.service.NodeFactory;
import com.cgi.eoss.fstep.io.LocalXfsPersistentFolderProvider;
import com.cgi.eoss.fstep.io.PersistentFolderMounter;
import com.cgi.eoss.fstep.io.PersistentFolderProvider;
import com.cgi.eoss.fstep.io.ServiceInputOutputManager;
import com.cgi.eoss.fstep.io.ServiceInputOutputManagerImpl;
import com.cgi.eoss.fstep.io.WorkerLocalPersistentFolderMounter;
import com.cgi.eoss.fstep.io.download.CachingSymlinkDownloaderFacade;
import com.cgi.eoss.fstep.io.download.Downloader;
import com.cgi.eoss.fstep.io.download.DownloaderFacade;
import com.cgi.eoss.fstep.io.download.UnzipStrategy;
import com.cgi.eoss.fstep.queues.QueuesConfig;
import com.cgi.eoss.fstep.rpc.FstepServerClient;
import com.cgi.eoss.fstep.rpc.InProcessRpcConfig;
import com.cgi.eoss.fstep.worker.jobs.WorkerJobDataService;
import com.cgi.eoss.fstep.worker.worker.DockerEventCollectorNodePreparer;
import com.cgi.eoss.fstep.worker.worker.DockerEventsListener;
import com.cgi.eoss.fstep.worker.worker.FstepWorkerNodeManager;
import com.cgi.eoss.fstep.worker.worker.JobEnvironmentService;
import com.cgi.eoss.fstep.worker.worker.LocalEventCollectorNodePreparer;
import com.cgi.eoss.fstep.worker.worker.NodePreparer;
import com.google.common.base.Strings;
import okhttp3.OkHttpClient;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

@Configuration
@ComponentScan(
        basePackageClasses = {WorkerConfig.class, Downloader.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = FstepWorkerApplication.class)
)
@Import({
		QueuesConfig.class,
        CloudsConfig.class,
        InProcessRpcConfig.class
})
@EnableEurekaClient
@EnableScheduling
@EnableConfigurationProperties(UnzipBySchemeProperties.class)
public class WorkerConfig {

    @Bean
    public Path cacheRoot(@Value("${fstep.worker.cache.baseDir:/data/cache/dl}") String cacheRoot) {
        return Paths.get(cacheRoot);
    }
    
    @Bean
    public Path persistentFoldersRoot(@Value("${fstep.worker.data.persistentDir:/data/persistentFolders}") String persistentFoldersRoot) {
        return Paths.get(persistentFoldersRoot);
    }

    @Bean
    public Boolean unzipAllDownloads(@Value("${ftep.worker.io.unzipAllDownloads:true}") boolean unzipAllDownloads) {
        return unzipAllDownloads;
    }
   
    @Bean
    public Integer cacheConcurrencyLevel(@Value("${fstep.worker.cache.concurrency:4}") int concurrencyLevel) {
        return concurrencyLevel;
    }

    @Bean
    public Integer cacheMaxWeight(@Value("${fstep.worker.cache.maxWeight:1024}") int maximumWeight) {
        return maximumWeight;
    }

    @Bean
    public Path jobEnvironmentRoot(@Value("${fstep.worker.jobEnv.baseDir:/data/cache/jobs}") String jobEnvRoot) {
        return Paths.get(jobEnvRoot);
    }
    
    @Bean
    public Integer maxJobsPerNode(@Value("${fstep.worker.maxJobsPerNode:2}") int maxJobsPerNode) {
        return maxJobsPerNode;
    }

    @Bean
    public Integer minWorkerNodes(@Value("${fstep.worker.minWorkerNodes:1}") int minWorkerNodes) {
        return minWorkerNodes;
    }

    @Bean
    public Integer maxWorkerNodes(@Value("${fstep.worker.maxWorkerNodes:1}") int maxWorkerNodes) {
        return maxWorkerNodes;
    }
    
    @Bean
    public Long minSecondsBetweenScalingActions(@Value("${fstep.worker.minSecondsBetweenScalingActions:600}") long minSecondsBetweenScalingActions) {
        return minSecondsBetweenScalingActions;
    }
    
    @Bean
    public Long minimumHourFractionUptimeSeconds(@Value("${fstep.worker.minimumHourFractionUptimeSeconds:3000}") long minimumHourFractionUptimeSeconds) {
        return minimumHourFractionUptimeSeconds;
    }

    @Bean
    public String workerId(@Value("${eureka.instance.metadataMap.workerId:workerId}") String workerId) {
        return workerId;
    }
    
    @Bean
    @ConditionalOnProperty("fstep.worker.dockerRegistryUrl")
    public DockerRegistryConfig dockerRegistryConfig(
            @Value("${fstep.worker.dockerRegistryUrl}") String dockerRegistryUrl,
            @Value("${fstep.worker.dockerRegistryUsername}") String dockerRegistryUsername,
            @Value("${fstep.worker.dockerRegistryPassword}") String dockerRegistryPassword) {
        return new DockerRegistryConfig(dockerRegistryUrl, dockerRegistryUsername, dockerRegistryPassword);
    }

    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // If an http_proxy is set in the current environment, add it to the client
        String httpProxy = System.getenv("http_proxy");
        if (!Strings.isNullOrEmpty(httpProxy)) {
            URI proxyUri = URI.create(httpProxy);
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));
        }

        return builder.build();
    }

    @Bean
    public FstepServerClient fstepServerClient(DiscoveryClient discoveryClient,
                                               @Value("${fstep.worker.server.eurekaServiceId:fs-tep server}") String fstepServerServiceId) {
        return new FstepServerClient(discoveryClient, fstepServerServiceId);
    }

    @Bean
    public DownloaderFacade downloaderFacade(@Qualifier("cacheRoot") Path cacheRoot,
                                             @Qualifier("unzipAllDownloads") Boolean unzipAllDownloads,
                                             UnzipBySchemeProperties unzipBySchemeProperties,
                                             @Qualifier("cacheConcurrencyLevel") Integer concurrencyLevel,
                                             @Qualifier("cacheMaxWeight") Integer maximumWeight) {
        return new CachingSymlinkDownloaderFacade(cacheRoot, UnzipStrategy.UNZIP_IN_SAME_FOLDER, unzipBySchemeProperties.getUnzipByScheme(), concurrencyLevel, maximumWeight);
    }

    @Bean
    public ServiceInputOutputManager serviceInputOutputManager(FstepServerClient ftepServerClient, DownloaderFacade downloaderFacade) {
        return new ServiceInputOutputManagerImpl(ftepServerClient, downloaderFacade);
    }
    
    @Bean
    public FstepWorkerNodeManager workerNodeManager(NodeFactory nodeFactory, @Qualifier("cacheRoot") Path dataBaseDir, JobEnvironmentService jobEnvironmentService,
            @Qualifier("maxJobsPerNode") Integer maxJobsPerNode, WorkerJobDataService workerJobDataService, @Autowired(required = false) NodePreparer nodePreparer) {
        FstepWorkerNodeManager workerNodeManager = new FstepWorkerNodeManager(nodeFactory, dataBaseDir, maxJobsPerNode, workerJobDataService, nodePreparer);
        return workerNodeManager;
    }
    
    @Bean
    public TaskScheduler taskScheduler() {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        return scheduler;
    }
    
    @Bean
    public PersistentFolderProvider persistentFolderProvider(@Qualifier("persistentFoldersRoot") Path persistentFoldersRoot) {
        return new LocalXfsPersistentFolderProvider(persistentFoldersRoot);
    }
    
    @Bean
    public PersistentFolderMounter persistentFolderMounter(@Qualifier("workerId") String workerId, @Qualifier("persistentFoldersRoot") Path persistentFoldersRoot) {
        return new WorkerLocalPersistentFolderMounter(workerId, persistentFoldersRoot);
    }

    @Value("${fstep.worker.updatesBrokerUrl:vm://embeddedBroker}")
    private String updatesBrokerUrl;
    
    @Value("${fstep.worker.updatesBrokerUsername:fstepbroker}")
    private String updatesBrokerUsername;
    
    @Value("${fstep.worker.updatesBrokerPassword:fstepbroker}")
    private String updatesBrokerPassword;
    
    @Value("${fstep.worker.updatesImageName:fs-tep-docker-event-collector}")
    private String updatesImageName;
    
    @Bean(name = "dockerEventListenerFactory")
    public DefaultJmsListenerContainerFactory dockerEventListenerFactory() {
      DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
      factory.setConnectionFactory(dockerEventActiveMQConnectionFactory());
      factory.setConcurrency("1-1");
      return factory;
    }
    
    @Bean(name = "dockerEventActiveMQConnectionFactory")
    public ActiveMQConnectionFactory dockerEventActiveMQConnectionFactory() {
      ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
      activeMQConnectionFactory.setUserName(updatesBrokerUsername);
      activeMQConnectionFactory.setPassword(updatesBrokerPassword);
      activeMQConnectionFactory.setBrokerURL(updatesBrokerUrl);
      activeMQConnectionFactory.setTrustedPackages(new ArrayList<>(Arrays.asList("shadow.dockerjava,java.util,java.lang".split(","))));
      return activeMQConnectionFactory;
    }
    
    @Bean
    @ConditionalOnProperty(name="fstep.worker.dockerEventCollector", havingValue = "true", matchIfMissing = false)
    public NodePreparer dockerNodePreparer(@Autowired(required = false) DockerRegistryConfig dockerRegistryConfig) {
    	return new DockerEventCollectorNodePreparer(dockerRegistryConfig, updatesImageName, updatesBrokerUrl, updatesBrokerUsername, updatesBrokerPassword, DockerEventsListener.DOCKER_EVENTS_QUEUE);
    }
    
    @Bean
    @ConditionalOnProperty(name="fstep.worker.localEventCollector", havingValue = "true", matchIfMissing = true)
    public NodePreparer localNodePreparer(@Qualifier(value = "dockerEventsJmsTemplate") JmsTemplate jmsTemplate) {
    	return new LocalEventCollectorNodePreparer(jmsTemplate, DockerEventsListener.DOCKER_EVENTS_QUEUE);
    }
    
    @Bean(name = "dockerEventsJmsTemplate")
    @ConditionalOnProperty(name="fstep.worker.localEventCollector", havingValue = "true", matchIfMissing = true)
    public JmsTemplate dockerEventsJmsTemplate() {
      JmsTemplate jmsTemplate = new JmsTemplate(new PooledConnectionFactory(dockerEventActiveMQConnectionFactory())) {
          @Override
        protected void doSend(MessageProducer producer, Message message) throws JMSException {
            producer.send(message, getDeliveryMode(), message.getJMSPriority(), getTimeToLive());
        }
      };
      jmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
      return jmsTemplate;
    }

}
