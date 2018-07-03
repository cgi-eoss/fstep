package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.FstepServiceTemplateFile;

public interface ServiceTemplateFilesApiCustom {
    <S extends FstepServiceTemplateFile> S save(S service);
}
