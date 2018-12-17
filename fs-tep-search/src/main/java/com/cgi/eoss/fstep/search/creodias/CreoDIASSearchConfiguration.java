package com.cgi.eoss.fstep.search.creodias;

import com.cgi.eoss.fstep.catalogue.external.ExternalProductDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "fstep.search.creodias.enabled", havingValue = "true", matchIfMissing = true)
public class CreoDIASSearchConfiguration {

    @Value("${fstep.search.creodias.baseUrl:https://finder.creodias.eu/resto/}")
    private String baseUrl;
    @Value("${fstep.search.creodias.username:}")
    private String username;
    @Value("${fstep.search.creodias.password:}")
    private String password;
    @Value("${fstep.search.creodias.priority:1}")
    private int priority;

    @Bean
    public CreoDIASSearchProvider creodiasSearchProvider(OkHttpClient httpClient, ObjectMapper objectMapper, ExternalProductDataService externalProductService) {
        return new CreoDIASSearchProvider(priority,
                CreoDIASSearchProperties.builder()
                        .baseUrl(HttpUrl.parse(baseUrl))
                        .username(username)
                        .password(password)
                        .build(),
                httpClient,
                objectMapper,
                externalProductService);
    }

}
