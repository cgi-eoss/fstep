package com.cgi.eoss.fstep.search.fstep;

import com.cgi.eoss.fstep.catalogue.resto.RestoService;
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
    public FstepSearchProvider restoSearchProvider(OkHttpClient httpClient, ObjectMapper objectMapper, RestoService restoService) {
        return new FstepSearchProvider(
                FstepSearchProperties.builder()
                        .baseUrl(HttpUrl.parse(baseUrl))
                        .username(username)
                        .password(password)
                        .build(),
                httpClient,
                objectMapper,
                restoService);
    }

}
