package com.cgi.eoss.fstep.api.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.cgi.eoss.fstep.model.SubscriptionPlan;
import com.cgi.eoss.fstep.persistence.dao.SubscriptionPlanDao;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Getter
@Component
public class SubscriptionPlansApiImpl {

    private final SubscriptionPlanDao dao;
    
    public Page<SubscriptionPlan> findAll(Pageable pageable) {
        return dao.findAll(pageable);
    }

    public <S extends SubscriptionPlan> S save(S subscriptionPlan) {
        return dao.save(subscriptionPlan);
    }
    
    public void delete(SubscriptionPlan subscriptionPlan) {
          dao.delete(subscriptionPlan);
    }
}
