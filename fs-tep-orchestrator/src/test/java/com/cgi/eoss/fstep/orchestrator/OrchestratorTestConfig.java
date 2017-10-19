package com.cgi.eoss.fstep.orchestrator;

import static org.mockito.Mockito.mock;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

@Configuration
public class OrchestratorTestConfig {

    @Bean
    public DiscoveryClient discoveryClient() {
        return mock(DiscoveryClient.class);
    }

    @Bean
    public InProcessServerBuilder serverBuilder() {
        return InProcessServerBuilder.forName(getClass().getName()).directExecutor();
    }

    @Bean
    public ManagedChannelBuilder channelBuilder() {
        return InProcessChannelBuilder.forName(getClass().getName()).directExecutor();
    }

}
