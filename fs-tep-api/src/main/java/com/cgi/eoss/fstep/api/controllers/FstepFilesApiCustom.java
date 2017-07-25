package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FstepFilesApiCustom {
    void delete(FstepFile fstepFile);

    Page<FstepFile> findByType(FstepFile.Type type, Pageable pageable);

    Page<FstepFile> findByFilterOnly(String filter, FstepFile.Type type, Pageable pageable);

    Page<FstepFile> findByFilterAndOwner(String filter, FstepFile.Type type, User user, Pageable pageable);

    Page<FstepFile> findByFilterAndNotOwner(String filter, FstepFile.Type type, User user, Pageable pageable);
}
