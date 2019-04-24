package com.cgi.eoss.fstep.model.projections;

import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

import com.cgi.eoss.fstep.model.SubscriptionPlan;

/**
 * <p>Abbreviated representation of a SubscriptionPlan entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortSubscriptionPlan", types = {SubscriptionPlan.class})
public interface ShortSubscriptionPlan extends Identifiable<Long> {
    String getName();
    String getDescription();
}
