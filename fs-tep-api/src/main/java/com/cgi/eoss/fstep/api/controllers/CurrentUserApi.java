package com.cgi.eoss.fstep.api.controllers;

import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.cgi.eoss.fstep.model.Group;
import com.cgi.eoss.fstep.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/currentUser")
public class CurrentUserApi {

    private final FstepSecurityService fstepSecurityService;

    @Autowired
    public CurrentUserApi(FstepSecurityService fstepSecurityService) {
        this.fstepSecurityService = fstepSecurityService;
    }

    @GetMapping
    public User currentUser() {
        return fstepSecurityService.getCurrentUser();
    }

    @GetMapping("/grantedAuthorities")
    public List<String> grantedAuthorities() {
        return fstepSecurityService.getCurrentAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    @GetMapping("/groups")
    public Set<Group> groups() {
        return fstepSecurityService.getCurrentGroups();
    }

}
