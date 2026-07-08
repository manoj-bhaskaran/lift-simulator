package com.liftsimulator.admin.config;

import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link RateLimitingFilter} as a servlet filter before the Spring Security
 * filter chain so that unauthenticated requests are also subject to rate limiting.
 *
 * <p>{@link EnableConfigurationProperties} ensures {@link RateLimitingProperties} is bound
 * and available as a bean in all Spring contexts, including {@code @WebMvcTest} slices.
 */
@Configuration
@EnableConfigurationProperties({RateLimitingProperties.class, RequestSizeLimitProperties.class})
public class RateLimitingConfig {

    /**
     * Registers the rate-limiting filter at one order before the Spring Security filter chain
     * ({@code SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1}) so it runs before authentication
     * and can throttle all traffic including unauthenticated brute-force or flood requests.
     */
    @Bean
    public FilterRegistrationBean<RequestSizeLimitFilter> requestSizeLimitFilterRegistration(
            RequestSizeLimitProperties properties) {
        FilterRegistrationBean<RequestSizeLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestSizeLimitFilter(properties));
        registration.addUrlPatterns("/api/v1/*", "/actuator/*");
        registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 2);
        registration.setName("requestSizeLimitFilter");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitingFilter> rateLimitingFilterRegistration(
            RateLimitingProperties properties) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitingFilter(properties));
        registration.addUrlPatterns("/api/v1/*", "/actuator/*");
        registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1);
        registration.setName("rateLimitingFilter");
        return registration;
    }
}
