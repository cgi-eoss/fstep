package com.cgi.eoss.fstep.worker;

import lombok.Data;

@Data
public class DockerRegistryConfig {
    
    private final String dockerRegistryUrl;
    
    private final String dockerRegistryUsername;
    
    private final String dockerRegistryPassword;
    
}
