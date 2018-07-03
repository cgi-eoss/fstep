package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QFstepServiceTemplate.fstepServiceTemplate;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.FstepServiceTemplateDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaServiceTemplateDataService extends AbstractJpaDataService<FstepServiceTemplate> implements ServiceTemplateDataService {

    private final FstepServiceTemplateDao fstepServiceTemplateDao;

    @Autowired
    public JpaServiceTemplateDataService(FstepServiceTemplateDao fstepServiceTemplateDao) {
        this.fstepServiceTemplateDao = fstepServiceTemplateDao;
    }

    @Override
    FstepEntityDao<FstepServiceTemplate> getDao() {
        return fstepServiceTemplateDao;
    }

    @Override
    Predicate getUniquePredicate(FstepServiceTemplate entity) {
        return fstepServiceTemplate.name.eq(entity.getName());
    }

    @Override
    public List<FstepServiceTemplate> search(String term) {
        return fstepServiceTemplateDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public List<FstepServiceTemplate> findByOwner(User user) {
        return fstepServiceTemplateDao.findByOwner(user);
    }

    @Override
    public FstepServiceTemplate getByName(String serviceTemplateName) {
        return fstepServiceTemplateDao.findOne(fstepServiceTemplate.name.eq(serviceTemplateName));
    }

}
