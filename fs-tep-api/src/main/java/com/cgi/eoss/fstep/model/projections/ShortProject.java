package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.security.FstepAccess;
import com.cgi.eoss.fstep.model.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

@Projection(name = "shortProject", types = {Project.class})
public interface ShortProject extends Identifiable<Long> {
    String getName();
    String getDescription();
    ShortUser getOwner();
    @Value("#{target.databaskets.size()}")
    Integer getDatabasketsCount();
    @Value("#{target.services.size()}")
    Integer getServicesCount();
    @Value("#{target.jobConfigs.size()}")
    Integer getJobConfigsCount();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.Project), target.id)}")
    FstepAccess getAccess();
}
