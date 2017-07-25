package com.cgi.eoss.fstep.orchestrator;

import com.cgi.eoss.fstep.catalogue.CatalogueConfig;
import com.cgi.eoss.fstep.costing.CostingConfig;
import com.cgi.eoss.fstep.orchestrator.service.FstepServiceLauncher;
import com.cgi.eoss.fstep.orchestrator.service.WorkerFactory;
import com.cgi.eoss.fstep.persistence.PersistenceConfig;
import com.cgi.eoss.fstep.persistence.service.WorkerLocatorExpressionDataService;
import com.cgi.eoss.fstep.rpc.InProcessRpcConfig;
import com.cgi.eoss.fstep.security.SecurityConfig;
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

/**
 * <p>Spring configuration for the FS-TEP Orchestrator component.</p>
 * <p>Manages access to distributed workers and provides the {@link FstepServiceLauncher} RPC service.</p>
 */
@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,

        CatalogueConfig.class,
        CostingConfig.class,
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
    public WorkerFactory workerFactory(DiscoveryClient discoveryClient,
                                       @Value("${fstep.orchestrator.worker.eurekaServiceId:fs-tep worker}") String workerServiceId,
                                       ExpressionParser workerLocatorExpressionParser,
                                       WorkerLocatorExpressionDataService workerLocatorExpressionDataService,
                                       @Value("${fstep.orchestrator.worker.defaultWorkerExpression:\"LOCAL\"}") String defaultWorkerExpression) {
        return new WorkerFactory(discoveryClient, workerServiceId, workerLocatorExpressionParser, workerLocatorExpressionDataService, defaultWorkerExpression);
    }

}
