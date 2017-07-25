package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.FstepServiceDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.cgi.eoss.fstep.model.QFstepService.fstepService;

@Service
@Transactional(readOnly = true)
public class JpaServiceDataService extends AbstractJpaDataService<FstepService> implements ServiceDataService {

    private final FstepServiceDao fstepServiceDao;

    @Autowired
    public JpaServiceDataService(FstepServiceDao fstepServiceDao) {
        this.fstepServiceDao = fstepServiceDao;
    }

    @Override
    FstepEntityDao<FstepService> getDao() {
        return fstepServiceDao;
    }

    @Override
    Predicate getUniquePredicate(FstepService entity) {
        return fstepService.name.eq(entity.getName());
    }

    @Override
    public List<FstepService> search(String term) {
        return fstepServiceDao.findByNameContainingIgnoreCase(term);
    }

    @Override
    public List<FstepService> findByOwner(User user) {
        return fstepServiceDao.findByOwner(user);
    }

    @Override
    public FstepService getByName(String serviceName) {
        return fstepServiceDao.findOne(fstepService.name.eq(serviceName));
    }

    @Override
    public List<FstepService> findAllAvailable() {
        return fstepServiceDao.findByStatus(FstepService.Status.AVAILABLE);
    }

}
