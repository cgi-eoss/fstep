package com.cgi.eoss.fstep.persistence.service;

import java.util.Set;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepFilesRelation;
import com.cgi.eoss.fstep.model.FstepFilesRelation.Type;

public interface FstepFilesRelationDataService extends
        FstepEntityDataService<FstepFilesRelation> {

	Set<FstepFilesRelation> findBySourceFileAndType(FstepFile fstepFile, Type relationType);
	
	Set<FstepFilesRelation> findByTargetFileAndType(FstepFile fstepFile, Type relationType);
   
}
