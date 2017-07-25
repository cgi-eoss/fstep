package com.cgi.eoss.fstep.search.fstep;

import com.cgi.eoss.fstep.catalogue.resto.RestoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FstepRestoSearchConfiguration {

    @Value("${fstep.search.resto.baseUrl:http://fstep-resto/resto}")
    private String baseUrl;
    @Value("${fstep.search.resto.username:}")
    private String username;
    @Value("${fstep.search.resto.password:}")
    private String password;

    @Bean
    public FstepRestoSearchProvider restoSearchProvider(OkHttpClient httpClient, ObjectMapper objectMapper, RestoService restoService) {
        return new FstepRestoSearchProvider(
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
