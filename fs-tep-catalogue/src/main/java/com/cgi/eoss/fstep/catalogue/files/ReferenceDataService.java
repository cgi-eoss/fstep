package com.cgi.eoss.fstep.catalogue.files;

import com.cgi.eoss.fstep.catalogue.FstepFileService;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface ReferenceDataService extends FstepFileService {
    FstepFile ingest(User user, String filename, String geometry, Map<String, Object> properties, MultipartFile multipartFile) throws IOException;
}
