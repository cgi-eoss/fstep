package com.cgi.eoss.fstep.api.controllers;

import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resources;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cgi.eoss.fstep.model.FstepService;
import com.cgi.eoss.fstep.persistence.service.ServiceDataService;
import com.cgi.eoss.fstep.services.DefaultFstepServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@BasePathAwareController
@RequestMapping("/services")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class ServicesApiExtension {

    private final ServiceDataService serviceDataService;

    @GetMapping("/defaults")
    public Resources<FstepService> getDefaultServices() {
        // Use the default service list, but retrieve updated objects from the database
        return new Resources<>(DefaultFstepServices.getDefaultServices().stream()
                .map(s -> serviceDataService.getByName(s.getName()))
                .collect(Collectors.toList()));
    }

}