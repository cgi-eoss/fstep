package com.cgi.eoss.fstep.model.internal;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OutputFileMetadata {
        private OutputProductMetadata outputProductMetadata;
        private String crs;
        private String geometry;
        private OffsetDateTime startDateTime;
        private OffsetDateTime endDateTime;
        
}
