package com.cgi.eoss.fstep.orchestrator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.cgi.eoss.fstep.catalogue.CatalogueConfig;
import com.cgi.eoss.fstep.costing.CostingConfig;
import com.cgi.eoss.fstep.orchestrator.service.FstepServiceLauncher;
import com.cgi.eoss.fstep.orchestrator.service.CachingWorkerFactory;
import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.cgi.eoss.fstep.persistence.service.WorkerLocatorExpressionDataService;
import com.cgi.eoss.fstep.queues.QueuesConfig;
import com.cgi.eoss.fstep.rpc.InProcessRpcConfig;
import com.cgi.eoss.fstep.security.SecurityConfig;

/**
 * <p>Spring configuration for the FS-TEP Orchestrator component.</p>
 * <p>Manages access to distributed workers and provides the {@link FstepServiceLauncher} RPC service.</p>
 */
@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,

        CatalogueConfig.class,
        CostingConfig.class,
        QueuesConfig.class,
        InProcessRpcConfig.class,
        PersistenceConfig.class,
        SecurityConfig.class
})
@EnableEurekaClient
@ComponentScan(basePackageClasses = OrchestratorConfig.class)
public class OrchestratorConfig {

    @Bean
    public ExpressionParser workerLocatorExpressionParser() {
        return new SpelExpressionParser();
    }

    @Bean
    public CachingWorkerFactory workerFactory(DiscoveryClient discoveryClient,
                                       @Value("${fstep.orchestrator.worker.eurekaServiceId:fs-tep worker}") String workerServiceId,
                                       ExpressionParser workerLocatorExpressionParser,
                                       WorkerLocatorExpressionDataService workerLocatorExpressionDataService,
                                       @Value("${fstep.orchestrator.worker.defaultWorkerExpression:\"LOCAL\"}") String defaultWorkerExpression) {
        return new CachingWorkerFactory(discoveryClient, workerServiceId, workerLocatorExpressionParser, workerLocatorExpressionDataService, defaultWorkerExpression);
    }

}
