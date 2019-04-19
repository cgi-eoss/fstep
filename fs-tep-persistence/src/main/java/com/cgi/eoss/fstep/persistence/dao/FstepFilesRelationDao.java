package com.cgi.eoss.fstep.persistence.dao;

import java.util.Set;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepFilesRelation;
import com.cgi.eoss.fstep.model.FstepFilesRelation.Type;

public interface FstepFilesRelationDao extends FstepEntityDao<FstepFilesRelation> {

	Set<FstepFilesRelation> findBySourceFileAndType(FstepFile fstepFile, Type relationType);

	Set<FstepFilesRelation> findByTargetFileAndType(FstepFile fstepFile, Type relationType);

}
