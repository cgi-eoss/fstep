package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.model.Subscription;

public interface SubscriptionsApiCustom {
    
    <S extends Subscription> S save(S subscription);
    
    void delete(Subscription subscription);
}
