package com.cgi.eoss.fstep.model.internal;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * <p>Convenience wrapper of metadata for a output data product.</p>
 */
@Data
@Builder
public class OutputProductMetadata {

    private User owner;
    private FstepService service;
    private String outputId;
    private String jobId;
    private Map<String, Object> productProperties;

}
