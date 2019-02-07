package com.cgi.eoss.fstep.model.projections;

import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;
import com.cgi.eoss.fstep.model.Job;
import com.cgi.eoss.fstep.model.JobConfig;
import com.cgi.eoss.fstep.security.FstepAccess;
import com.google.common.collect.Multimap;

/**
 * <p>Comprehensive representation of a Job entity, including outputs and jobConfig, for embedding in
 * REST responses.</p>
 */
@Projection(name = "detailedJob", types = Job.class)
public interface DetailedJob extends Identifiable<Long> {
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
    Multimap<String, String> getOutputs();
    Set<ShortFstepFile> getOutputFiles();
    JobConfig getConfig();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.Job), target.id)}")
    FstepAccess getAccess();
}
