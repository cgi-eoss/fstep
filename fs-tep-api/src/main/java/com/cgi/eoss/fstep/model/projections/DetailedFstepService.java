package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.security.FstepAccess;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Comprehensive representation of an FstepService entity, including the full description of input and output fields, for embedding in REST
 * responses.</p>
 */
@Projection(name = "detailedFstepService", types = FstepService.class)
public interface DetailedFstepService extends Identifiable<Long> {

    String getName();
    String getDescription();
    ShortUser getOwner();
    FstepService.Type getType();
    String getDockerTag();
    FstepService.Licence getLicence();
    FstepService.Status getStatus();
    FstepServiceDescriptor getServiceDescriptor();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.FstepService), target.id)}")
    FstepAccess getAccess();

}
