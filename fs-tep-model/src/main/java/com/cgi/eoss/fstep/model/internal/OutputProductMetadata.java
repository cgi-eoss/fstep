package com.cgi.eoss.fstep.model.internal;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.User;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * <p>Convenience wrapper of metadata for a reference data product.</p>
 */
@Data
@Builder
public class OutputProductMetadata {

    private User owner;
    private FstepService service;
    private String jobId;
    private String crs;
    private String geometry;
    private Map<String, Object> properties;

}
