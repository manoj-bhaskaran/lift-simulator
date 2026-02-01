package com.liftsimulator.admin.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers API key authentication for runtime and simulation execution endpoints.
 */
@Configuration
public class ApiKeyAuthConfiguration {

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilter(
            @Value("${api.auth.key:}") String apiKey,
            @Value("${api.auth.header:X-API-Key}") String headerName) {
        FilterRegistrationBean<ApiKeyAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiKeyAuthFilter(apiKey, headerName));
        registration.addUrlPatterns(
                "/api/runtime/*",
                "/api/runtime/*/*",
                "/api/runtime/*/*/*",
                "/api/runtime/*/*/*/*",
                "/api/simulation-runs",
                "/api/simulation-runs/*",
                "/api/simulation-runs/*/*");
        registration.setOrder(1);
        return registration;
    }
}
