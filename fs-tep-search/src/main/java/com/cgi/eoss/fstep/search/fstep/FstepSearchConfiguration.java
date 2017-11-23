package com.cgi.eoss.fstep.search.fstep;

import com.cgi.eoss.fstep.catalogue.CatalogueService;
import com.cgi.eoss.fstep.catalogue.resto.RestoService;
import com.cgi.eoss.fstep.persistence.service.CollectionDataService;
import com.cgi.eoss.fstep.persistence.service.FstepFileDataService;
import com.cgi.eoss.fstep.security.FstepSecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "fstep.search.fstep.enabled", havingValue = "true", matchIfMissing = true)
public class FstepSearchConfiguration {

    @Value("${fstep.search.fstep.baseUrl:http://fstep-resto/resto}")
    private String baseUrl;
    @Value("${fstep.search.fstep.username:}")
    private String username;
    @Value("${fstep.search.fstep.password:}")
    private String password;

    @Bean
    public FstepSearchProvider fstepSearchProvider(OkHttpClient httpClient, ObjectMapper objectMapper, CatalogueService catalogueService, RestoService restoService, FstepFileDataService fstepFileDataService, FstepSecurityService securityService, CollectionDataService collectionDataService) {
        return new FstepSearchProvider(0,
                FstepSearchProperties.builder()
                        .baseUrl(HttpUrl.parse(baseUrl))
                        .username(username)
                        .password(password)
                        .build(),
                httpClient,
                objectMapper,
                catalogueService,
                restoService,
                fstepFileDataService,
                securityService,
                collectionDataService);
    }

}
