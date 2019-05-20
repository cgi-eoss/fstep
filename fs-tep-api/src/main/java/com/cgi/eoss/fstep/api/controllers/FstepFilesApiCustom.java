package com.cgi.eoss.fstep.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepFile.Type;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.User;

public interface FstepFilesApiCustom {
    void delete(FstepFile fstepFile);

    Page<FstepFile> findByType(FstepFile.Type type, Pageable pageable);

    Page<FstepFile> findByFilterOnly(String filter, FstepFile.Type type, Pageable pageable);

    Page<FstepFile> findByFilterAndOwner(String filter, FstepFile.Type type, User user, Pageable pageable);

    Page<FstepFile> findByFilterAndNotOwner(String filter, FstepFile.Type type, User user, Pageable pageable);
	
	Page<FstepFile> parametricFind(String filter, Collection collection, Type type, User user, User notOwner, Job job,
			Pageable pageable);
}
