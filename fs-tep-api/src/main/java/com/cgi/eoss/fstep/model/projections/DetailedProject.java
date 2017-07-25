package com.cgi.eoss.fstep.model.projections;

import com.cgi.eoss.fstep.security.FstepAccess;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.model.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import java.util.Set;

/**
 * <p>Comprehensive representation of a Project entity for embedding in REST responses.</p>
 */
@Projection(name = "detailedProject", types = {Project.class})
public interface DetailedProject extends Identifiable<Long> {
    String getName();
    String getDescription();
    ShortUser getOwner();
    Set<ShortDatabasket> getDatabaskets();
    Set<ShortFstepService> getServices();
    Set<JobConfig> getJobConfigs();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.Project), target.id)}")
    FstepAccess getAccess();
}
