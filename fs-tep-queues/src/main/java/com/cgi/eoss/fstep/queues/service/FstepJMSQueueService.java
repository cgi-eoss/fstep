package com.cgi.eoss.fstep.queues.service;

import java.util.Enumeration;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.jms.support.converter.MessageConversionException;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class FstepJMSQueueService implements FstepQueueService {


    private JmsTemplate jmsTemplate;

    @Autowired
    public FstepJMSQueueService(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }
    
    
    @Override
    public void sendObject(String queueName, Object object) {
        jmsTemplate.convertAndSend(queueName, object);
    }
    
    @Override
    public void sendObject(String queueName, Object object, int priority) {
        jmsTemplate.convertAndSend(queueName, object, new MessagePostProcessor() {
            
            @Override
            public Message postProcessMessage(Message message) throws JMSException {
                message.setJMSPriority(priority);
                return message;
            }
        });
    }
   
    @Override
    public void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object) {
        jmsTemplate.convertAndSend(queueName, object, new MessagePostProcessor() {

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
        jmsTemplate.convertAndSend(queueName, object, new MessagePostProcessor() {

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
    public Object receiveObject(String queueName) {
        jmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
        return jmsTemplate.receiveAndConvert(queueName);
    }
    
    @Override
    public Object receiveObjectWithTimeout(String queueName, long timeout) {
        jmsTemplate.setReceiveTimeout(timeout);
        return jmsTemplate.receiveAndConvert(queueName);
    }

    @Override
    public Object receiveSelectedObject(String queueName, String messageSelector) {
        jmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
        return jmsTemplate.receiveSelectedAndConvert(queueName, messageSelector);
    }
    
    @Override
    public com.cgi.eoss.fstep.queues.service.Message receiveSelected(String queueName, String messageSelector) {
        jmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
        Message jmsMessage = jmsTemplate.receiveSelected(queueName, messageSelector);
        com.cgi.eoss.fstep.queues.service.Message m = new com.cgi.eoss.fstep.queues.service.Message();
        try {
			Object payload = jmsTemplate.getMessageConverter().fromMessage(jmsMessage);
			m.setPayload(payload);
			m.setPriority(jmsMessage.getJMSPriority());
			Enumeration e = jmsMessage.getPropertyNames();
			while (e.hasMoreElements()) {
				String propertyName = (String) e.nextElement();
				m.getHeaders().put(propertyName, jmsMessage.getObjectProperty(propertyName));
			}
			return m;
		} catch (MessageConversionException | JMSException e) {
			throw new MessageConversionException(e.getMessage());
		}
        
    }
    
    @Override
    public Object receiveSelectedObjectWithTimeout(String queueName, String messageSelector, long timeout) {
        jmsTemplate.setReceiveTimeout(timeout);
        return jmsTemplate.receiveSelectedAndConvert(queueName, messageSelector);
    }
    
    @Override
    public com.cgi.eoss.fstep.queues.service.Message receive(String queueName) {
        jmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
        Message jmsMessage = jmsTemplate.receive(queueName);
        com.cgi.eoss.fstep.queues.service.Message m = new com.cgi.eoss.fstep.queues.service.Message();
        try {
			Object payload = jmsTemplate.getMessageConverter().fromMessage(jmsMessage);
			m.setPayload(payload);
			m.setPriority(jmsMessage.getJMSPriority());
			Enumeration e = jmsMessage.getPropertyNames();
			while (e.hasMoreElements()) {
				String propertyName = (String) e.nextElement();
				m.getHeaders().put(propertyName, jmsMessage.getObjectProperty(propertyName));
			}
			return m;
		} catch (MessageConversionException | JMSException e) {
			throw new MessageConversionException(e.getMessage());
		}
        
    }
    
    @Override
    public com.cgi.eoss.fstep.queues.service.Message receiveWithTimeout(String queueName, long timeout) {
        jmsTemplate.setReceiveTimeout(timeout);
        Message jmsMessage = jmsTemplate.receive(queueName);
        if (jmsMessage == null) {
        	return null;
        }
        com.cgi.eoss.fstep.queues.service.Message m = new com.cgi.eoss.fstep.queues.service.Message();
        try {
			Object payload = jmsTemplate.getMessageConverter().fromMessage(jmsMessage);
			m.setPayload(payload);
			m.setPriority(jmsMessage.getJMSPriority());
			Enumeration e = jmsMessage.getPropertyNames();
			while (e.hasMoreElements()) {
				String propertyName = (String) e.nextElement();
				m.getHeaders().put(propertyName, jmsMessage.getObjectProperty(propertyName));
			}
			return m;
		} catch (MessageConversionException | JMSException e) {
			throw new MessageConversionException(e.getMessage());
		}
    }

    @Override
    public long getQueueLength(String queueName) {
        return jmsTemplate.execute(session -> {
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
}
