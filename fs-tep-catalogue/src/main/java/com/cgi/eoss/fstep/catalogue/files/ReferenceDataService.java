package com.cgi.eoss.fstep.catalogue.files;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.springframework.hateoas.Link;
import org.springframework.web.multipart.MultipartFile;

import com.cgi.eoss.fstep.catalogue.FstepFileService;
import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.model.internal.FstepFileIngestion;
import com.cgi.eoss.fstep.model.internal.UploadableFileType;

public interface ReferenceDataService extends FstepFileService {
    FstepFileIngestion ingest(String collection, User user, String filename, UploadableFileType filetype, Map<String, Object> properties, MultipartFile multipartFile) throws IOException;

    public String getDefaultCollection();

    void createCollection(Collection collection) throws IOException;

    void deleteCollection(Collection collection) throws IOException;

	Set<Link> getOGCLinks(FstepFile fstepFile);
}
