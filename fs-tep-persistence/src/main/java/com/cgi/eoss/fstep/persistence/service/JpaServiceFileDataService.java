package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceContextFile;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.FstepServiceContextFileDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.fstep.model.QFstepServiceContextFile.fstepServiceContextFile;

@Service
@Transactional(readOnly = true)
public class JpaServiceFileDataService extends AbstractJpaDataService<FstepServiceContextFile> implements ServiceFileDataService {

    private final FstepServiceContextFileDao fstepServiceContextFileDao;

    @Autowired
    public JpaServiceFileDataService(FstepServiceContextFileDao fstepServiceContextFileDao) {
        this.fstepServiceContextFileDao = fstepServiceContextFileDao;
    }

    @Override
    FstepEntityDao<FstepServiceContextFile> getDao() {
        return fstepServiceContextFileDao;
    }

    @Override
    Predicate getUniquePredicate(FstepServiceContextFile entity) {
        return fstepServiceContextFile.service.eq(entity.getService()).and(fstepServiceContextFile.filename.eq(entity.getFilename()));
    }

    @Override
    public List<FstepServiceContextFile> findByService(FstepService service) {
        return fstepServiceContextFileDao.findByService(service);
    }

}
