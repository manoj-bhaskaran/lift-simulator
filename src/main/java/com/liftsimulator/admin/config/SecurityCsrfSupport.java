package com.liftsimulator.admin.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Shared CSRF configuration applied identically to every security filter chain.
 *
 * <p>CSRF is disabled by default for the stateless REST APIs. When
 * {@code security.csrf.enabled} is set, a cookie-based token repository is used
 * with the paths in {@code security.csrf.ignored-paths} exempted.
 */
final class SecurityCsrfSupport {

    private SecurityCsrfSupport() {
    }

    /**
     * Configures CSRF for a filter chain based on the explicit
     * {@code security.csrf} configuration.
     */
    static void configure(CsrfConfigurer<HttpSecurity> csrf, CsrfProperties csrfProperties) {
        if (!csrfProperties.isEnabled()) {
            // Intentional: CSRF is disabled by default for the stateless REST APIs and is
            // toggled explicitly via security.csrf.enabled (see ADR-0022). This mirrors the
            // pre-existing behaviour prior to the SecurityConfig split.
            csrf.disable(); // codeql[java/spring-disabled-csrf-protection]
            return;
        }

        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        PathPatternRequestMatcher.Builder matcherBuilder = PathPatternRequestMatcher.withDefaults();
        RequestMatcher[] ignoredMatchers = csrfProperties.getIgnoredPaths().stream()
            .map(matcherBuilder::matcher)
            .toArray(RequestMatcher[]::new);

        csrf.csrfTokenRepository(tokenRepository)
            .csrfTokenRequestHandler(requestHandler)
            .ignoringRequestMatchers(ignoredMatchers);
    }
}
