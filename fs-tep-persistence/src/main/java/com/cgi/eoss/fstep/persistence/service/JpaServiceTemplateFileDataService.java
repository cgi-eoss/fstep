package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QFstepServiceTemplateFile.fstepServiceTemplateFile;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.FstepServiceTemplateFile;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.FstepServiceTemplateFileDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaServiceTemplateFileDataService extends AbstractJpaDataService<FstepServiceTemplateFile> implements ServiceTemplateFileDataService {

    private final FstepServiceTemplateFileDao fstepServiceTemplateFileDao;

    @Autowired
    public JpaServiceTemplateFileDataService(FstepServiceTemplateFileDao fstepServiceTemplateFileDao) {
        this.fstepServiceTemplateFileDao = fstepServiceTemplateFileDao;
    }

    @Override
    FstepEntityDao<FstepServiceTemplateFile> getDao() {
        return fstepServiceTemplateFileDao;
    }

    @Override
    Predicate getUniquePredicate(FstepServiceTemplateFile entity) {
        return fstepServiceTemplateFile.serviceTemplate.eq(entity.getServiceTemplate()).and(fstepServiceTemplateFile.filename.eq(entity.getFilename()));
    }

    @Override
    public List<FstepServiceTemplateFile> findByServiceTemplate(FstepServiceTemplate serviceTemplate) {
        return fstepServiceTemplateFileDao.findByServiceTemplate(serviceTemplate);
    }

}
