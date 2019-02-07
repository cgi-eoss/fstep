package com.cgi.eoss.fstep.model.projections;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.security.FstepAccess;

/**
 * <p>Default JSON projection for embedded {@link Job}s. Embeds the owner as a ShortUser.</p>
 */
@Projection(name = "shortJob", types = {Job.class})
public interface ShortJob extends Identifiable<Long> {
    String getExtId();
    ShortUser getOwner();
    Job.Status getStatus();
    String getGuiUrl();
    String getStage();
    LocalDateTime getStartTime();
    LocalDateTime getEndTime();
    Integer getQueuePosition();
    boolean isParent();
    @Value("#{target.config.service.name}")
    String getServiceName();
    @Value("#{target.config.systematicParameter}")
    String getSystematicParameter();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.Job), target.id)}")
    FstepAccess getAccess();
}
