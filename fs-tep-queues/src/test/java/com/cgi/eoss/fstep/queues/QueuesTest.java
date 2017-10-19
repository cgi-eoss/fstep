package com.cgi.eoss.fstep.queues;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.cgi.eoss.fstep.queues.service.FstepQueueService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {QueuesConfig.class, QueuesTestConfig.class})
@TestPropertySource("classpath:test-queues.properties")
public class QueuesTest {

    private static final String TEST_QUEUE = "test_queue";
    
    @Autowired
    private FstepQueueService queueService;
    
    @Test
    public void testQueueLength() {
        String sentMessage = "Test message";
        
        queueService.sendObject(TEST_QUEUE, sentMessage);
        queueService.receiveObject(TEST_QUEUE);
        long queueLength = queueService.getQueueLength(TEST_QUEUE);
        
        assertThat(queueLength == 0);
        
        queueService.sendObject(TEST_QUEUE, sentMessage);
        
        queueLength = queueService.getQueueLength(TEST_QUEUE);
        assertThat(queueLength == 1);
        
        String received_message = (String) queueService.receiveObject(TEST_QUEUE);
        
        assertThat(received_message.equals(sentMessage));
        
        queueLength = queueService.getQueueLength(TEST_QUEUE);
        
        assertThat(queueLength == 0);
        
    }
    
}
