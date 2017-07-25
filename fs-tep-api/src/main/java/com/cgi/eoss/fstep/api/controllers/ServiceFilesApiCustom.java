package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.FstepServiceContextFile;

public interface ServiceFilesApiCustom {
    <S extends FstepServiceContextFile> S save(S service);
}
