package com.cgi.eoss.fstep.clouds.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Node {
    private final String id;
    private final String name;
    private final String tag;
    private final String dockerEngineUrl;
    public String ipAddress;
}
