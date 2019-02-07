package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QQuota.quota;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.QuotaDao;
import com.querydsl.core.types.Predicate;

@Service
@Transactional(readOnly = true)
public class JpaQuotaDataService extends AbstractJpaDataService<Quota> implements QuotaDataService {

    private final QuotaDao dao;

   
    @Autowired
    public JpaQuotaDataService(QuotaDao quotaDao) {
        this.dao = quotaDao;
    }

    @Override
    FstepEntityDao<Quota> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(Quota entity) {
    	return quota.usageType.eq(entity.getUsageType()).and(quota.owner.eq(entity.getOwner()));
    }

    @Override
    public Quota getByOwnerAndUsageType(User user, UsageType usageType) {
    	return dao.getByOwnerAndUsageType(user, usageType);
    }

}
