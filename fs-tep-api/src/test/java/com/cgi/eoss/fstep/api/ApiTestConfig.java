package com.cgi.eoss.fstep.api;

import static org.mockito.Mockito.mock;
import javax.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.BeanFactoryDestinationResolver;

@Configuration
public class ApiTestConfig {

    @Bean
    public DiscoveryClient discoveryClient() {
        return mock(DiscoveryClient.class);
    }
 
    @Autowired
    private BeanFactory springContextBeanFactory;
    
    @Bean
    public JmsTemplate jmsTemplate(){
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(connectionFactory());
        return template;
    }

    @Bean
    public ConnectionFactory connectionFactory(){
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        return connectionFactory;
    }
    
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory =
                new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDestinationResolver(new BeanFactoryDestinationResolver(springContextBeanFactory));
        factory.setConcurrency("3-10");
        return factory;
    }

}
