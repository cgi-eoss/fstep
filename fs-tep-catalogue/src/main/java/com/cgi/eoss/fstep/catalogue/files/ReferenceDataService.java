package com.cgi.eoss.fstep.catalogue.files;

import com.cgi.eoss.fstep.catalogue.FstepFileService;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.internal.UploadableFileType;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface ReferenceDataService extends FstepFileService {
    FstepFile ingest(User user, String filename, UploadableFileType filetype, Map<String, Object> properties, MultipartFile multipartFile) throws IOException;
}
