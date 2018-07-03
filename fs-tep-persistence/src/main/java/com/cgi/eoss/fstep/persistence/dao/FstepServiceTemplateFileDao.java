package com.cgi.eoss.fstep.persistence.dao;

import java.util.List;

import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.FstepServiceTemplateFile;

public interface FstepServiceTemplateFileDao extends FstepEntityDao<FstepServiceTemplateFile> {
    List<FstepServiceTemplateFile> findByServiceTemplate(FstepServiceTemplate serviceTemplate);
}
