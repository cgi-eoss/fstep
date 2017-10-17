package com.cgi.eoss.fstep.queues.service;

import java.util.Map;

public interface FstepQueueService {

    final static String jobQueueName = "fstep-jobs";

    final static String jobUpdatesQueueName = "fstep-jobs-updates";
    
    void sendObject(String queueName, Object object);

    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object);

    public Object receiveObject(String queueName);
    
    public Object receiveObjectWithTimeout(String queueName, long timeout);
   
    public Object receiveSelectedObject(String queueName, String messageSelector);
    
    public Object receiveSelectedObjectWithTimeout(String queueName, String messageSelector, long timeout);

    public long getQueueLength(String queueName);

}
