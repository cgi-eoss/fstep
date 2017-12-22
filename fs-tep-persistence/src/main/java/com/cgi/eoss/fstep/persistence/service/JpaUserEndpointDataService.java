package com.cgi.eoss.fstep.persistence.service;

import static com.cgi.eoss.fstep.model.QUserEndpoint.userEndpoint;
import com.cgi.eoss.fstep.model.UserEndpoint;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.UserEndpointDao;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JpaUserEndpointDataService extends AbstractJpaDataService<UserEndpoint>
        implements UserEndpointDataService {

    private final UserEndpointDao dao;

    @Autowired
    public JpaUserEndpointDataService(UserEndpointDao userEndpointDao) {
        this.dao = userEndpointDao;
    }
    
    @Override
    FstepEntityDao<UserEndpoint> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(UserEndpoint entity) {
        return userEndpoint.name.eq(entity.getName())
                .and(userEndpoint.owner.eq(entity.getOwner()));
    }
  
}
