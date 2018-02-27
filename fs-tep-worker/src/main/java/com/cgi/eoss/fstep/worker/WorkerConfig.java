package com.cgi.eoss.fstep.worker;

import com.cgi.eoss.fstep.clouds.CloudsConfig;
import com.cgi.eoss.fstep.clouds.service.NodeFactory;
import com.cgi.eoss.fstep.io.ServiceInputOutputManager;
import com.cgi.eoss.fstep.io.ServiceInputOutputManagerImpl;
import com.cgi.eoss.fstep.io.download.CachingSymlinkDownloaderFacade;
import com.cgi.eoss.fstep.io.download.Downloader;
import com.cgi.eoss.fstep.io.download.DownloaderFacade;
import com.cgi.eoss.fstep.queues.QueuesConfig;
import com.cgi.eoss.fstep.rpc.FstepServerClient;
import com.cgi.eoss.fstep.rpc.InProcessRpcConfig;
import com.cgi.eoss.fstep.worker.worker.FstepWorkerNodeManager;
import com.cgi.eoss.fstep.worker.worker.JobEnvironmentService;
import com.google.common.base.Strings;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

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
public class WorkerConfig {

    @Bean
    public Path cacheRoot(@Value("${fstep.worker.cache.baseDir:/data/cache/dl}") String cacheRoot) {
        return Paths.get(cacheRoot);
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
                                             @Qualifier("cacheConcurrencyLevel") Integer concurrencyLevel,
                                             @Qualifier("cacheMaxWeight") Integer maximumWeight) {
        return new CachingSymlinkDownloaderFacade(cacheRoot, unzipAllDownloads, concurrencyLevel, maximumWeight);
    }

    @Bean
    public ServiceInputOutputManager serviceInputOutputManager(FstepServerClient ftepServerClient, DownloaderFacade downloaderFacade) {
        return new ServiceInputOutputManagerImpl(ftepServerClient, downloaderFacade);
    }
    
    @Bean
    public FstepWorkerNodeManager workerNodeManager(NodeFactory nodeFactory, @Qualifier("cacheRoot") Path dataBaseDir, JobEnvironmentService jobEnvironmentService,
            @Qualifier("maxJobsPerNode") Integer maxJobsPerNode) {
        FstepWorkerNodeManager workerNodeManager = new FstepWorkerNodeManager(nodeFactory, dataBaseDir, maxJobsPerNode);
        return workerNodeManager;
    }
    
    @Bean
    public TaskScheduler taskScheduler() {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        return scheduler;
    }



}
