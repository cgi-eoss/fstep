package com.cgi.eoss.fstep.model.projections;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import com.cgi.eoss.fstep.model.Subscription;
import com.cgi.eoss.fstep.model.Subscription.Status;
import com.cgi.eoss.fstep.security.FstepAccess;

/**
 * <p>Abbreviated representation of a Subscription entity, for embedding in REST responses.</p>
 */
@Projection(name = "detailedSubscription", types = {Subscription.class})
public interface DetailedSubscription extends Identifiable<Long> {
	ShortUser getOwner();
    ShortSubscriptionPlan getSubscriptionPlan();
    Integer getQuantity();
    OffsetDateTime getCreated();
    OffsetDateTime getEnded();
    OffsetDateTime getCurrentStart();
    OffsetDateTime getCurrentEnd();
    boolean isRenew();
    Status getStatus();
    Integer getDowngradeQuantity();
    ShortSubscriptionPlan getDowngradePlan();
    @Value("#{@fstepSecurityService.getCurrentAccess(T(com.cgi.eoss.fstep.model.Subscription), target.id)}")
    FstepAccess getAccess();
}
