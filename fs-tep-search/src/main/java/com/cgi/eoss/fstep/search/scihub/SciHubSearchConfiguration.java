package com.cgi.eoss.fstep.search.scihub;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SciHubSearchConfiguration {

    @Value("${fstep.search.scihub.baseUrl:https://scihub.copernicus.eu/apihub/search}")
    private String baseUrl;
    @Value("${fstep.search.scihub.username:}")
    private String username;
    @Value("${fstep.search.scihub.password:}")
    private String password;

    @Bean
    public SciHubSearchProvider sciHubSearchProvider(OkHttpClient httpClient) {
        return new SciHubSearchProvider(SciHubSearchProperties.builder()
                .baseUrl(HttpUrl.parse(baseUrl))
                .username(username)
                .password(password)
                .build(), httpClient);
    }

}
