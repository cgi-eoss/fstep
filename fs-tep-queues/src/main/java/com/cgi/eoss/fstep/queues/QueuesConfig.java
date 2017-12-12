package com.cgi.eoss.fstep.queues;

import java.net.URI;
import java.util.Arrays;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.plugin.StatisticsBrokerPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
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
    
    @Value("${spring.activemq.broker-url:vm://embeddedBroker}")
    private String brokerUrl;
    
    @Value("${spring.activemq.user:admin}")
    private String brokerUserName;
    
    @Value("${spring.activemq.password:admin}")
    private String brokerPassword;
 
    
    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory() {
      ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
      activeMQConnectionFactory.setUserName(brokerUserName);
      activeMQConnectionFactory.setPassword(brokerPassword);
      activeMQConnectionFactory.setTrustedPackages(Arrays.asList("com.google.protobuf"));
      activeMQConnectionFactory.setBrokerURL(brokerUrl);

      return activeMQConnectionFactory;
    }
    
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

    @Bean
    public JmsTemplate jmsTemplate() {
      return new JmsTemplate(new CachingConnectionFactory(activeMQConnectionFactory())) {
          @Override
        protected void doSend(MessageProducer producer, Message message) throws JMSException {
            producer.send(message, getDeliveryMode(), message.getJMSPriority(), getTimeToLive());
        }
      };
    }
   
    
    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() {
      DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
      factory.setConnectionFactory(activeMQConnectionFactory());
      factory.setConcurrency("1-1");
      return factory;
    }
    

}
