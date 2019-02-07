package com.cgi.eoss.fstep.queues.service;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    private Object payload;
    
    private Integer priority;
    
    private Map<String, Object> headers = new HashMap<>();

}
