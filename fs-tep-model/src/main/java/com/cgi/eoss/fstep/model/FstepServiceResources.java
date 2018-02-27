package com.cgi.eoss.fstep.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FstepServiceResources {

    private String cpus;
    
    private String ram;

    private String storage;
}
