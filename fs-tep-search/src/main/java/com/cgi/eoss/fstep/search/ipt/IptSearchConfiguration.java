package com.cgi.eoss.fstep.search.ipt;

import com.cgi.eoss.fstep.catalogue.external.ExternalProductDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "fstep.search.ipt.enabled", havingValue = "true", matchIfMissing = true)
public class IptSearchConfiguration {

    @Value("${fstep.search.ipt.baseUrl:https://finder.eocloud.eu/resto/}")
    private String baseUrl;
    @Value("${fstep.search.ipt.username:}")
    private String username;
    @Value("${fstep.search.ipt.password:}")
    private String password;

    @Bean
    public IptSearchProvider iptSearchProvider(OkHttpClient httpClient, ObjectMapper objectMapper, ExternalProductDataService externalProductService) {
        return new IptSearchProvider(0,
                IptSearchProperties.builder()
                        .baseUrl(HttpUrl.parse(baseUrl))
                        .username(username)
                        .password(password)
                        .build(),
                httpClient,
                objectMapper,
                externalProductService);
    }

}
