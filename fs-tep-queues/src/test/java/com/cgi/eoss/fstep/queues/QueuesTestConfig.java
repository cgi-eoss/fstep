package com.cgi.eoss.fstep.queues;

import java.net.URI;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

public class QueuesTestConfig {

    @Value("${spring.activemq.broker-url:vm://localhost?persistent=false}")
    private String brokerUrl;
    
    @Bean(name= "brokerService", initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(value = "spring.activemq.broker-url", havingValue = "vm://embeddedBroker", matchIfMissing = false)
    public BrokerService brokerService() throws Exception {
        BrokerService broker = new BrokerService();
        broker.setBrokerName("embeddedBroker");
        broker.setPlugins(new BrokerPlugin[]{new StatisticsBrokerPlugin()});
        broker.setPersistent(false);
        TransportConnector connector = new TransportConnector();
        connector.setUri(new URI(brokerUrl));
        broker.addConnector(connector);
        broker.start();
        return broker;
    }
    
}
