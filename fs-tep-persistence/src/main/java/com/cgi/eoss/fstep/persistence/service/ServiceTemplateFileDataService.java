package com.cgi.eoss.fstep.persistence.service;

import java.util.List;

import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.FstepServiceTemplateFile;

public interface ServiceTemplateFileDataService extends
        FstepEntityDataService<FstepServiceTemplateFile> {
    List<FstepServiceTemplateFile> findByServiceTemplate(FstepServiceTemplate serviceTemplate);
}
