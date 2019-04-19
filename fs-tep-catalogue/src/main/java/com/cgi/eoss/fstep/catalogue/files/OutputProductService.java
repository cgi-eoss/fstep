package com.cgi.eoss.fstep.catalogue.files;

import com.cgi.eoss.fstep.catalogue.FstepFileService;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.hateoas.Link;

public interface OutputProductService extends FstepFileService {
    
    FstepFile ingest(String collection, User owner, String jobId, String crs, String geometry, OffsetDateTime startDateTime, OffsetDateTime endDateTime, Map<String, Object> properties,
            Path path) throws IOException;
    
    String getDefaultCollection();
    
    void createCollection(Collection collection) throws IOException;

    void deleteCollection(Collection collection) throws IOException;

    Path provision(String jobId, String filename) throws IOException;

    Set<Link> getOGCLinks(FstepFile fstepFile);


}
