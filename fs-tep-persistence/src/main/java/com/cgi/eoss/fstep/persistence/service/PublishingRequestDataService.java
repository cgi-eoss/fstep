package com.cgi.eoss.fstep.persistence.service;

import com.cgi.eoss.fstep.model.Collection;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.model.PublishingRequest;
import com.cgi.eoss.fstep.model.User;

import java.util.List;

public interface PublishingRequestDataService extends
        FstepEntityDataService<PublishingRequest> {
    List<PublishingRequest> findByOwner(User user);

    List<PublishingRequest> findRequestsForPublishing(FstepService service);
    
    List<PublishingRequest> findRequestsForPublishingServiceTemplate(FstepServiceTemplate serviceTemplate);
    
    List<PublishingRequest> findRequestsForPublishingCollection(Collection collection);

    List<PublishingRequest> findOpenByAssociated(Class<?> objectClass, Long identifier);
}
