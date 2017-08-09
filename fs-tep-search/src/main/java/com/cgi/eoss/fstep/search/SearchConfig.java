package com.cgi.eoss.fstep.search;

import com.cgi.eoss.fstep.catalogue.CatalogueConfig;
import com.cgi.eoss.fstep.search.api.SearchFacade;
import com.cgi.eoss.fstep.search.api.SearchProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Collection;

@Configuration
@Import({
        PropertyPlaceholderAutoConfiguration.class,

        CatalogueConfig.class
})
@ComponentScan(basePackageClasses = SearchConfig.class)
public class SearchConfig {

    @Bean
    public SearchFacade searchFacade(Collection<SearchProvider> searchProviders) {
        return new SearchFacade(searchProviders);
    }

    @Bean
    public OkHttpClient httpClient() {
        return new OkHttpClient.Builder().build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModules(new GuavaModule(), new JavaTimeModule());
    }

}
