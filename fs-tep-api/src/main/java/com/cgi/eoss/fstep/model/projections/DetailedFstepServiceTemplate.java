package com.cgi.eoss.fstep.model.projections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.model.FstepServiceDescriptor;
import com.cgi.eoss.fstep.model.FstepServiceTemplate;
import com.cgi.eoss.fstep.security.FstepAccess;

/**
 * <p>Comprehensive representation of an FstepServiceTemplate entity, including the full description of input and output fields, for embedding in REST
 * responses.</p>
 */
@Projection(name = "detailedFstepServiceTemplate", types = FstepServiceTemplate.class)
public interface DetailedFstepServiceTemplate extends Identifiable<Long> {

    String getName();
    String getDescription();
    ShortUser getOwner();
    FstepService.Type getType();
    FstepServiceDescriptor getServiceDescriptor();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.FstepServiceTemplate), target.id)}")
    FstepAccess getAccess();

}
