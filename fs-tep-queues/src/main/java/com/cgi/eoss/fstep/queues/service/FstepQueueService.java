package com.cgi.eoss.fstep.queues.service;

import java.util.Map;

public interface FstepQueueService {

    final static String jobExecutionQueueName = "fstep-jobs";
    
    final static String jobPendingQueueName = "fstep-pending-jobs";

    final static String jobUpdatesQueueName = "fstep-jobs-updates";
    
    void sendObject(String queueName, Object object);

    void sendObject(String queueName, Object object, int priority);

    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object);
    
    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object, int priority);

    void send(String queueName, Message message);
    
    public Object receiveObject(String queueName);
    
    public Object receiveObjectNoWait(String queueName);
   
    public Object receiveSelectedObject(String queueName, String messageSelector);
    
    public Object receiveSelectedObjectNoWait(String queueName, String messageSelector);

    public long getQueueLength(String queueName);

	public Message receiveSelected(String queueName, String messageSelector);
	
	public Message receive(String queueName);
	
	public Message receiveNoWait(String queueName);

	public void browse(String queueName, BrowserClient browserClient);

}
