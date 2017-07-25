package com.cgi.eoss.fstep.catalogue;

import com.cgi.eoss.fstep.model.FstepFile;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 */
public interface FstepFileService {
    Resource resolve(FstepFile file);
    void delete(FstepFile file) throws IOException;
}
