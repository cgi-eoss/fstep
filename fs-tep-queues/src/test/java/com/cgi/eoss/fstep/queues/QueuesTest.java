package com.cgi.eoss.fstep.queues;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cgi.eoss.fstep.queues.service.BrowserClient;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import com.cgi.eoss.fstep.queues.service.Message;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {QueuesConfig.class})
@TestPropertySource("classpath:test-queues.properties")
public class QueuesTest {

    private static final String TEST_QUEUE = "test_queue";
    
    private static final String TEST_QUEUE_1 = "test_queue_1";
    
    private static final String TEST_QUEUE_2 = "test_queue_2";
    
    @Autowired
    private FstepQueueService queueService;
    
    @Test
    public void testSendWithPriority() {
        String firstMessage = "First message";
        String secondMessage = "Second message";
        queueService.sendObject(TEST_QUEUE, firstMessage, 1);
        queueService.sendObject(TEST_QUEUE, secondMessage, 5);
        String received_message = (String) queueService.receiveObject(TEST_QUEUE);
        assertTrue(received_message.equals(secondMessage));
    }
    
    @Test
    public void testReceiveSelected() {
        String payload = "First message";
        Message message = new Message();
        message.setPayload(payload);
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("jobId", "alphaalpha");
        message.setHeaders(headers);
        queueService.send(TEST_QUEUE, message);
        String received_message = (String) queueService.receiveSelectedObject(TEST_QUEUE, "jobId = 'alphaalpha'");
        assertTrue(received_message.equals(payload));
    }
    
    @Test
    public void testQueueLength() {
        String sentMessage = "Test message";
        
        queueService.sendObject(TEST_QUEUE, sentMessage);
        queueService.receiveObject(TEST_QUEUE);
        long queueLength = queueService.getQueueLength(TEST_QUEUE);
        
        assertTrue(queueLength == 0);
        
        queueService.sendObject(TEST_QUEUE, sentMessage);
        
        queueLength = queueService.getQueueLength(TEST_QUEUE);
        
        assertTrue(queueLength == 1);
        
        String received_message = (String) queueService.receiveObject(TEST_QUEUE);
        
        assertTrue(received_message.equals(sentMessage));
        
        queueLength = queueService.getQueueLength(TEST_QUEUE);
        
        assertTrue(queueLength == 0);
        
    }
    
    @Test
    public void testMoveBetweenQueues() {
    	HashMap<String, Object> headers1 = new HashMap<>();
        headers1.put("jobId", "400");
        Message message1 = new Message("Test message 1", 1, headers1);
        HashMap<String, Object> headers2 = new HashMap<>();
        headers2.put("jobId", "402");
        Message message2 = new Message("Test message 2", 1, headers2);
        HashMap<String, Object> headers3 = new HashMap<>();
        headers3.put("jobId", "404");
        Message message3 = new Message("Test message 3", 1, headers3);
        
        queueService.send(TEST_QUEUE_1, message1);
        queueService.send(TEST_QUEUE_1, message2);
        queueService.send(TEST_QUEUE_1, message3);
        
        long queue1Length = queueService.getQueueLength(TEST_QUEUE_1);
        
        assertTrue(queue1Length == 3);
    	
        List<Message> waitingMessages = new ArrayList<>();
        
        queueService.browse(TEST_QUEUE_1, new BrowserClient() {
			
			@Override
			public boolean stopBrowsing() {
				return false;
			}
			
			@Override
			public void handleMessage(Message m) {
				if (canBeUnblocked(m)) waitingMessages.add(m);
				
			}
		});
        for (Message waitingMessage: waitingMessages) {
        	String jobId = (String) waitingMessage.getHeaders().get("jobId");
        	queueService.receiveSelected(TEST_QUEUE_1, "jobId = '" + jobId + "'");
        	queueService.send(TEST_QUEUE_2, waitingMessage);
        }
        queue1Length = queueService.getQueueLength(TEST_QUEUE_1);
        
        assertTrue(queue1Length == 2);
        
        long queue2Length = queueService.getQueueLength(TEST_QUEUE_2);
        
        assertTrue(queue2Length == 1);
        
        String received_message = (String) queueService.receiveObject(TEST_QUEUE_2);
        
        assertTrue(received_message.equals("Test message 2"));
        
    }
    
    private boolean canBeUnblocked(Message m) {
    	return m.getHeaders().get("jobId").equals("402");
    }
    
}
