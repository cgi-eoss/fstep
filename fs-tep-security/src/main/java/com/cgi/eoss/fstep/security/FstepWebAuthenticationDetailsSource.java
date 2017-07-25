package com.cgi.eoss.fstep.security;

import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import javax.servlet.http.HttpServletRequest;

public class FstepWebAuthenticationDetailsSource extends WebAuthenticationDetailsSource {

    private final String emailRequestHeader;

    public FstepWebAuthenticationDetailsSource(String emailRequestHeader) {
        super();
        this.emailRequestHeader = emailRequestHeader;
    }

    public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
        FstepWebAuthenticationDetails details = new FstepWebAuthenticationDetails(context);
        details.setUserEmail(context.getHeader(emailRequestHeader));
        return details;
    }

}
