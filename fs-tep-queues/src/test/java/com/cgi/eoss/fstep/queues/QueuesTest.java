package com.cgi.eoss.fstep.queues;

import static org.junit.Assert.assertTrue;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {QueuesConfig.class})
@TestPropertySource("classpath:test-queues.properties")
public class QueuesTest {

    private static final String TEST_QUEUE = "test_queue";
    
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
    
}
