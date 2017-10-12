package com.cgi.eoss.fstep.queues.service;

import java.util.Map;
import javax.jms.JMSException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

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
    public void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object) {
        jmsTemplate.convertAndSend(queueName, object, new MessagePostProcessor() {

            @Override
            public javax.jms.Message postProcessMessage(javax.jms.Message message)
                    throws JMSException {
                additionalHeaders.forEach((k, v) -> {
                    try {
                        message.setObjectProperty(k, v);
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                });
                return message;
            }
        });
    }

    @Override
    public Object receiveObject(String queueName) {
        jmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
        return jmsTemplate.receiveAndConvert(queueName);
    }

    @Override
    public Object receiveSelectedObject(String queueName, String messageSelector, boolean wait) {
        if (wait) {
            jmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
        }
        else {
            jmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT);
        }
        return jmsTemplate.receiveSelectedAndConvert(queueName, messageSelector);
    }
    
   


}
