package com.cgi.eoss.fstep.api.controllers;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cgi.eoss.fstep.model.Quota;
import com.cgi.eoss.fstep.model.UsageType;
import com.cgi.eoss.fstep.persistence.dao.QuotaDao;
import com.cgi.eoss.fstep.security.FstepSecurityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@BasePathAwareController
@RequestMapping("/quotas")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class QuotasApiExtension {

    private final FstepSecurityService fstepSecurityService;
    private final QuotaDao dao;
    
    @GetMapping("/usageTypes")
    public List<UsageType> getUsageTypes(){
    	return Arrays.asList(UsageType.values());
    }
    
    @GetMapping("/value")
    public Long getQuotaValueByUsageType(@RequestParam UsageType usageType){
    	Quota userQuota = dao.getByOwnerAndUsageType(fstepSecurityService.getCurrentUser(), usageType);
    	if (userQuota != null) {
    		return userQuota.getValue();
    	}
    	return usageType.getDefaultValue();
    }
}
