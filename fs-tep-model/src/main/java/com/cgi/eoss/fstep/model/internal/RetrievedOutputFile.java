package com.cgi.eoss.fstep.model.internal;

import lombok.Data;
import java.nio.file.Path;

@Data
public class RetrievedOutputFile {
    
    private final OutputFileMetadata outputFileMetadata;
    
    private final Path path;

}
