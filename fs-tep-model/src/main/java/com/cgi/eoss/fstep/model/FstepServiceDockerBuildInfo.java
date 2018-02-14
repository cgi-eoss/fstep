package com.cgi.eoss.fstep.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FstepServiceDockerBuildInfo {
    
    private String lastBuiltFingerprint;
    
    private Status dockerBuildStatus = Status.NOT_STARTED;
    
    public enum Status {
        NOT_STARTED, ONGOING, COMPLETED, ERROR
    }
}
