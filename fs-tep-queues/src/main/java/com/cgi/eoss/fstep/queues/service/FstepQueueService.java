package com.cgi.eoss.fstep.queues.service;

import java.util.Map;

public interface FstepQueueService {

    final static String jobQueueName = "fstep-jobs";

    final static String jobUpdatesQueueName = "fstep-jobs-updates";
    
    void sendObject(String queueName, Object object);

    void sendObject(String queueName, Object object, int priority);

    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object);
    
    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object, int priority);

    public Object receiveObject(String queueName);
    
    public Object receiveObjectWithTimeout(String queueName, long timeout);
   
    public Object receiveSelectedObject(String queueName, String messageSelector);
    
    public Object receiveSelectedObjectWithTimeout(String queueName, String messageSelector, long timeout);

    public long getQueueLength(String queueName);

	public Message receiveSelected(String queueName, String messageSelector);
	
	public Message receive(String queueName);
	
	public Message receiveWithTimeout(String queueName, long timeout);

}
