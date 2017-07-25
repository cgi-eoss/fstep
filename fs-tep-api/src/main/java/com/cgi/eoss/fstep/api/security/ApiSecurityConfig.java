package com.cgi.eoss.fstep.api.security;

import com.cgi.eoss.fstep.security.FstepUserDetailsService;
import com.cgi.eoss.fstep.security.FstepWebAuthenticationDetailsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

@Configuration
@ConditionalOnProperty(value = "fstep.api.security.mode", havingValue = "SSO")
@EnableWebSecurity
@EnableGlobalAuthentication
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class ApiSecurityConfig {

    @Bean
    public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter(
            @Value("${fstep.api.security.username-request-header:REMOTE_USER}") String usernameRequestHeader,
            @Value("${fstep.api.security.email-request-header:REMOTE_EMAIL}") String emailRequestHeader) {
        return new WebSecurityConfigurerAdapter() {
            @Override
            protected void configure(HttpSecurity httpSecurity) throws Exception {
                // Extracts the shibboleth user id from the request
                RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
                filter.setAuthenticationManager(authenticationManager());
                filter.setPrincipalRequestHeader(usernameRequestHeader);
                filter.setAuthenticationDetailsSource(new FstepWebAuthenticationDetailsSource(emailRequestHeader));

                // Handles any authentication exceptions, and translates to a simple 403
                // There is no login redirection as we are expecting pre-auth
                ExceptionTranslationFilter exceptionTranslationFilter = new ExceptionTranslationFilter(new Http403ForbiddenEntryPoint());

                httpSecurity
                        .addFilterBefore(exceptionTranslationFilter, RequestHeaderAuthenticationFilter.class)
                        .addFilter(filter)
                        .authorizeRequests()
                        .anyRequest().authenticated();
                httpSecurity
                        .csrf().disable();
                httpSecurity
                        .cors();
                httpSecurity
                        .sessionManagement()
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            }
        };
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, FstepUserDetailsService fstepUserDetailsService) {
        PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(fstepUserDetailsService);
        auth.authenticationProvider(authenticationProvider);
    }

    @Bean
    public MethodSecurityExpressionHandler createExpressionHandler(AclPermissionEvaluator aclPermissionEvaluator) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(aclPermissionEvaluator);
        return expressionHandler;
    }

}
