package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QFstepFilesRelation.fstepFilesRelation;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.FstepFile;
import com.cgi.eoss.fstep.model.FstepFilesRelation;
import com.cgi.eoss.fstep.model.FstepFilesRelation.Type;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.FstepFilesRelationDao;
import com.querydsl.core.types.Predicate;
@Service
@Transactional(readOnly = true)
public class JpaFstepFilesRelationDataService extends AbstractJpaDataService<FstepFilesRelation> implements FstepFilesRelationDataService {

	
	private final FstepFilesRelationDao dao;
	 
	@Autowired
    public JpaFstepFilesRelationDataService(FstepFilesRelationDao fstepFileRelationDao) {
        this.dao = fstepFileRelationDao;
    }
	
	@Override
	FstepEntityDao<FstepFilesRelation> getDao() {
		return dao;
	}

	@Override
	Predicate getUniquePredicate(FstepFilesRelation entity) {
		return fstepFilesRelation.sourceFile.eq(entity.getSourceFile())
				.and(fstepFilesRelation.sourceFile.eq(entity.getTargetFile()))
				.and(fstepFilesRelation.type.eq(entity.getType()));
	}

	@Override
	public Set<FstepFilesRelation> findBySourceFileAndType(FstepFile fstepFile, Type relationType) {
		return dao.findBySourceFileAndType(fstepFile, relationType);
	}
	
	@Override
	public Set<FstepFilesRelation> findByTargetFileAndType(FstepFile fstepFile, Type relationType) {
		return dao.findByTargetFileAndType(fstepFile, relationType);
	}

    
}
