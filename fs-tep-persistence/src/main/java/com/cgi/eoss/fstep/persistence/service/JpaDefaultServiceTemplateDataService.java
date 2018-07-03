package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QDefaultServiceTemplate.defaultServiceTemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.DefaultServiceTemplate;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.persistence.dao.DefaultServiceTemplateDao;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaDefaultServiceTemplateDataService extends AbstractJpaDataService<DefaultServiceTemplate> implements DefaultServiceTemplateDataService {

    private final DefaultServiceTemplateDao defaultServiceTemplateDao;

    @Autowired
    public JpaDefaultServiceTemplateDataService(DefaultServiceTemplateDao defaultServiceTemplateDao) {
        this.defaultServiceTemplateDao = defaultServiceTemplateDao;
    }

    @Override
    FstepEntityDao<DefaultServiceTemplate> getDao() {
        return defaultServiceTemplateDao;
    }

    @Override
    Predicate getUniquePredicate(DefaultServiceTemplate entity) {
        return defaultServiceTemplate.serviceType.eq(entity.getServiceType());
    }

    @Override
    public DefaultServiceTemplate getByServiceType(FstepService.Type serviceType) {
        return defaultServiceTemplateDao.getByServiceType(serviceType);
    }

}
