package com.cgi.eoss.fstep.catalogue.files;

import com.cgi.eoss.fstep.catalogue.FstepFileService;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import okhttp3.HttpUrl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface OutputProductService extends FstepFileService {
    FstepFile ingest(User owner, String jobId, String crs, String geometry, Map<String, Object> properties, Path path) throws IOException;

    Path provision(String jobId, String filename) throws IOException;

    HttpUrl getWmsUrl(String jobId, String filename);
}
