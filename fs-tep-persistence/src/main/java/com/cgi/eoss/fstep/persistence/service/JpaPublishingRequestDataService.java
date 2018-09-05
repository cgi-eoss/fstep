package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.PublishingRequest;
import com.cgi.eoss.fstep.model.QPublishingRequest;
import com.cgi.eoss.fstep.model.User;
import com.cgi.eoss.fstep.persistence.dao.FstepEntityDao;
import com.cgi.eoss.fstep.persistence.dao.PublishingRequestDao;
import com.google.common.collect.ImmutableList;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class JpaPublishingRequestDataService extends AbstractJpaDataService<PublishingRequest> implements PublishingRequestDataService {

    private final PublishingRequestDao dao;

    @Autowired
    public JpaPublishingRequestDataService(PublishingRequestDao publishingRequestDao) {
        this.dao = publishingRequestDao;
    }

    @Override
    FstepEntityDao<PublishingRequest> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(PublishingRequest entity) {
        return QPublishingRequest.publishingRequest.owner.eq(entity.getOwner())
                .and(QPublishingRequest.publishingRequest.type.eq(entity.getType()))
                .and(QPublishingRequest.publishingRequest.associatedId.eq(entity.getAssociatedId()));
    }

    @Override
    public List<PublishingRequest> findByOwner(User user) {
        return dao.findByOwner(user);
    }

    @Override
    public List<PublishingRequest> findRequestsForPublishing(FstepService service) {
        return findOpenByAssociated(FstepService.class, service.getId());
    }
    
    @Override
    public List<PublishingRequest> findRequestsForPublishingServiceTemplate(FstepServiceTemplate serviceTemplate) {
        return findOpenByAssociated(FstepServiceTemplate.class, serviceTemplate.getId());
    }
    
    @Override
    public List<PublishingRequest> findRequestsForPublishingCollection(Collection collection) {
        return findOpenByAssociated(Collection.class, collection.getId());
    }

    @Override
    public List<PublishingRequest> findOpenByAssociated(Class<?> objectClass, Long identifier) {
        if (PublishingRequest.Type.of(objectClass) == null) {
            return ImmutableList.of();
        }

        return dao.findAll(QPublishingRequest.publishingRequest.type.eq(PublishingRequest.Type.of(objectClass))
                .and(QPublishingRequest.publishingRequest.associatedId.eq(identifier))
                .and(QPublishingRequest.publishingRequest.status.in(PublishingRequest.Status.REQUESTED, PublishingRequest.Status.NEEDS_INFO)));
    }

}
