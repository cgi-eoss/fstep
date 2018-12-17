package com.cgi.eoss.fstep.catalogue.files;

import java.io.IOException;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.cgi.eoss.fstep.catalogue.FstepFileService;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.internal.FstepFileIngestion;
import com.cgi.eoss.fstep.model.internal.UploadableFileType;

public interface ReferenceDataService extends FstepFileService {
    FstepFileIngestion ingest(User user, String filename, UploadableFileType filetype, Map<String, Object> properties, MultipartFile multipartFile) throws IOException;
}
