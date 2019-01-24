package com.cgi.eoss.fstep.queues.service;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class Message {

    private Object payload;
    
    private int priority;
    
    private Map<String, Object> headers = new HashMap<>();

}
