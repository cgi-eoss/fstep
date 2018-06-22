package com.cgi.eoss.fstep.catalogue.files;

import com.cgi.eoss.fstep.catalogue.FstepFileService;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import okhttp3.HttpUrl;

public interface OutputProductService extends FstepFileService {
    
    FstepFile ingest(String collection, User owner, String jobId, String crs, String geometry, OffsetDateTime startDateTime, OffsetDateTime endDateTime, Map<String, Object> properties,
            Path path) throws IOException;
    
    public String getDefaultCollection();
    
    public boolean createCollection(Collection collection);

    boolean deleteCollection(Collection collection);

    Path provision(String jobId, String filename) throws IOException;

    HttpUrl getWmsUrl(String jobId, String filename);


}
