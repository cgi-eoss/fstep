package com.cgi.eoss.fstep.queues;

import java.util.Arrays;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import com.cgi.eoss.fstep.queues.service.FstepJMSQueueService;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;

/**
 * <p>
 * Spring configuration for the FS-TEP Queues component.
 * </p>
 * <p>
 * Manages job and job updates queues
 * </p>
 */
@Configuration
@Import({PropertyPlaceholderAutoConfiguration.class})
@EnableJms
@ComponentScan(basePackageClasses = QueuesConfig.class)
public class QueuesConfig {

    @Bean
    public FstepQueueService queueService(JmsTemplate jmsTemplate) {
        return new FstepJMSQueueService(jmsTemplate);
    }

    @Value("${spring.activemq.broker-url:vm://localhost?broker.persistent=false}")
    private String brokerUrl;
 
    
    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory() {
      ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
      activeMQConnectionFactory.setTrustedPackages(Arrays.asList("com.google.protobuf"));
      activeMQConnectionFactory.setBrokerURL(brokerUrl);

      return activeMQConnectionFactory;
    }

    @Bean
    public JmsTemplate jmsTemplate() {
      return new JmsTemplate(new SingleConnectionFactory(activeMQConnectionFactory()));
    }
    
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
      DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
      factory.setConnectionFactory(activeMQConnectionFactory());
      factory.setConcurrency("3-10");
      return factory;
    }
    

}
