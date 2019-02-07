package com.cgi.eoss.fstep.queues.service;

import java.util.Enumeration;
import java.util.Map;
import java.util.function.Predicate;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.core.BrowserCallback;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class FstepJMSQueueService implements FstepQueueService {


    private JmsTemplate blockingJmsTemplate;
    private JmsTemplate nonblockingJmsTemplate;
    @Autowired
    public FstepJMSQueueService(@Qualifier("blockingJmsTemplate")JmsTemplate blockingJmsTemplate, @Qualifier("nonblockingJmsTemplate")JmsTemplate nonblockingJmsTemplate) {
        this.blockingJmsTemplate = blockingJmsTemplate;
        this.nonblockingJmsTemplate = nonblockingJmsTemplate;
        
    }
    
    
    @Override
    public void sendObject(String queueName, Object object) {
        blockingJmsTemplate.convertAndSend(queueName, object);
    }
    
    @Override
    public void sendObject(String queueName, Object object, int priority) {
    	blockingJmsTemplate.convertAndSend(queueName, object, new MessagePostProcessor() {
            
            @Override
            public Message postProcessMessage(Message message) throws JMSException {
                message.setJMSPriority(priority);
                return message;
            }
        });
    }
   
    @Override
    public void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object) {
    	blockingJmsTemplate.convertAndSend(queueName, object, new MessagePostProcessor() {

            @Override
            public javax.jms.Message postProcessMessage(javax.jms.Message message) throws JMSException {
                additionalHeaders.forEach((k, v) -> {
                    try {
                        message.setObjectProperty(k, v);
                    } catch (JMSException e) {
                        LOG.error("Error sending message to JMS Queue " + queueName, e);
                    }
                });
                return message;
            }
        });
    }
    
    @Override
    public void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object, int priority) {
    	blockingJmsTemplate.convertAndSend(queueName, object, new MessagePostProcessor() {

            @Override
            public javax.jms.Message postProcessMessage(javax.jms.Message message) throws JMSException {
                additionalHeaders.forEach((k, v) -> {
                    try {
                        message.setObjectProperty(k, v);
                    } catch (JMSException e) {
                        LOG.error("Error sending message to JMS Queue " + queueName, e);
                    }
                });
                message.setJMSPriority(priority);
                return message;
            }
        });
    }
    
    @Override
    public void send(String queueName, com.cgi.eoss.fstep.queues.service.Message message) {
    	if (message.getPriority() != null) {
    		sendObject(queueName, message.getHeaders(), message.getPayload(), message.getPriority());
    	}
    	else {
    		sendObject(queueName, message.getHeaders(), message.getPayload());
    	}
    	
    }

    @Override
    public Object receiveObject(String queueName) {
        return blockingJmsTemplate.receiveAndConvert(queueName);
    }
    
    @Override
    public Object receiveObjectNoWait(String queueName) {
        return nonblockingJmsTemplate.receiveAndConvert(queueName);
    }

    @Override
	public Object receiveSelectedObject(String queueName, String messageSelector) {
	    return blockingJmsTemplate.receiveSelectedAndConvert(queueName, messageSelector);
	}


	@Override
	public Object receiveSelectedObjectNoWait(String queueName, String messageSelector) {
	    return nonblockingJmsTemplate.receiveSelectedAndConvert(queueName, messageSelector);
	}


	@Override
	public com.cgi.eoss.fstep.queues.service.Message receive(String queueName) {
	    Message jmsMessage = blockingJmsTemplate.receive(queueName);
	    try {
			return fromJMSMessage(jmsMessage);
		} catch (MessageConversionException | JMSException e) {
			throw new MessageConversionException(e.getMessage());
		}
	    
	}


	@Override
    public com.cgi.eoss.fstep.queues.service.Message receiveSelected(String queueName, String messageSelector) {
        Message jmsMessage = blockingJmsTemplate.receiveSelected(queueName, messageSelector);
        try {
			return fromJMSMessage(jmsMessage);
		} catch (MessageConversionException | JMSException e) {
			throw new MessageConversionException(e.getMessage());
		}
        
    }
    
    private com.cgi.eoss.fstep.queues.service.Message fromJMSMessage(Message jmsMessage) throws JMSException {
		com.cgi.eoss.fstep.queues.service.Message m = new com.cgi.eoss.fstep.queues.service.Message();
		Object payload = blockingJmsTemplate.getMessageConverter().fromMessage(jmsMessage);
		m.setPayload(payload);
		m.setPriority(jmsMessage.getJMSPriority());
		Enumeration e = jmsMessage.getPropertyNames();
		while (e.hasMoreElements()) {
			String propertyName = (String) e.nextElement();
			m.getHeaders().put(propertyName, jmsMessage.getObjectProperty(propertyName));
		}
		return m;
	}
    
    @Override
    public com.cgi.eoss.fstep.queues.service.Message receiveNoWait(String queueName) {
        Message jmsMessage = nonblockingJmsTemplate.receive(queueName);
        if (jmsMessage == null) {
        	return null;
        }
        try {
			return fromJMSMessage(jmsMessage);
		} catch (MessageConversionException | JMSException e) {
			throw new MessageConversionException(e.getMessage());
		}
    }

    @Override
    public long getQueueLength(String queueName) {
        return blockingJmsTemplate.execute(session -> {
            String statisticsQueueName = "ActiveMQ.Statistics.Destination." + queueName;
            Queue statisticsQueue = session.createQueue(statisticsQueueName);
            Queue replyTo = session.createTemporaryQueue();
            MessageConsumer consumer = session.createConsumer(replyTo);
            MessageProducer producer = session.createProducer(statisticsQueue);
            Message msg = session.createMessage();
            msg.setJMSReplyTo(replyTo);
            producer.send(msg);
            MapMessage reply = (MapMessage) consumer.receive();
            long queueSize = reply.getLong("size");
            producer.close();
            consumer.close();
            return queueSize;
        }, true);

    }
    
    @Override
    public void browse(String queueName, BrowserClient browserClient) {

        this.blockingJmsTemplate.browse(queueName, new BrowserCallback<Void>() {
            public Void doInJms(final Session session, final QueueBrowser browser) {
				try {
					Enumeration enumeration;
					enumeration = browser.getEnumeration();
					 while (enumeration.hasMoreElements() && browserClient.stopBrowsing() == false) {
		               	Message msg = (Message) enumeration.nextElement();
		               	browserClient.handleMessage(fromJMSMessage(msg));
		             }
					 browser.close();
				} catch (JMSException e) {
					throw new UncategorizedJmsException(e);
				}
               
                return null;
            }
        });
    }
    
  
}
