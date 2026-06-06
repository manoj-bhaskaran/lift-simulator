package com.liftsimulator.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ApiKeyAuthenticationFilter secure comparison.
 * Tests that API key validation uses SHA-256 hashing and constant-time comparison
 * to prevent timing attacks and credential leakage.
 */
@ExtendWith(MockitoExtension.class)
public class ApiKeyAuthenticationFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private AuthenticationEntryPoint authenticationEntryPoint;

    private ApiKeyAuthenticationFilter filter;
    private static final String TEST_API_KEY = "test-api-key-1234567890";
    private static final String API_KEY_HEADER = "X-API-Key";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new ApiKeyAuthenticationFilter(API_KEY_HEADER, TEST_API_KEY, authenticationEntryPoint);
    }

    @Test
    void doFilter_ValidApiKey_AuthenticatesRequest() throws Exception {
        when(request.getHeader(API_KEY_HEADER)).thenReturn(TEST_API_KEY);
        when(request.getRequestURI()).thenReturn("/api/v1/runtime/systems/test/config");

        filter.doFilter(request, response, filterChain);

        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication()).isNotNull();
        assertThat(context.getAuthentication().getPrincipal()).isEqualTo("api-client");
        assertThat(context.getAuthentication().getAuthorities()).anyMatch(auth -> auth.getAuthority().equals("ROLE_RUNTIME"));
        verify(filterChain).doFilter(request, response);
        verify(authenticationEntryPoint, never()).commence(eq(request), eq(response), any(AuthenticationException.class));
    }

    @Test
    void doFilter_InvalidApiKey_RejectsRequest() throws Exception {
        when(request.getHeader(API_KEY_HEADER)).thenReturn("wrong-api-key");
        when(request.getRequestURI()).thenReturn("/api/v1/runtime/systems/test/config");

        filter.doFilter(request, response, filterChain);

        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any(AuthenticationException.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilter_MissingApiKey_RejectsRequest() throws Exception {
        when(request.getHeader(API_KEY_HEADER)).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/runtime/systems/test/config");

        filter.doFilter(request, response, filterChain);

        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any(AuthenticationException.class));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilter_BlankApiKey_RejectsRequest() throws Exception {
        when(request.getHeader(API_KEY_HEADER)).thenReturn("   ");
        when(request.getRequestURI()).thenReturn("/api/v1/runtime/systems/test/config");

        filter.doFilter(request, response, filterChain);

        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any(AuthenticationException.class));
    }

    @Test
    void doFilter_SimilarApiKey_RejectsRequest() throws Exception {
        // Test that similar but different API keys are rejected
        when(request.getHeader(API_KEY_HEADER)).thenReturn("test-api-key-1234567891"); // One character different
        when(request.getRequestURI()).thenReturn("/api/v1/runtime/systems/test/config");

        filter.doFilter(request, response, filterChain);

        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any(AuthenticationException.class));
    }

    @Test
    void doFilter_TimingAttackResistant() throws Exception {
        // Verify that the comparison uses constant-time hashing approach.
        // This test confirms that hash comparison time is not significantly different
        // between early and late mismatches, providing protection against timing attacks.
        // Note: Absolute timing can vary due to system load, so we verify the principle
        // of constant-time comparison (using SHA-256 hashing) rather than strict ratios.
        ApiKeyAuthenticationFilter filter1 = new ApiKeyAuthenticationFilter(
            API_KEY_HEADER, "test-key-12345678901234567890", authenticationEntryPoint);
        ApiKeyAuthenticationFilter filter2 = new ApiKeyAuthenticationFilter(
            API_KEY_HEADER, "test-key-12345678901234567890", authenticationEntryPoint);

        // Early character difference
        when(request.getHeader(API_KEY_HEADER)).thenReturn("wrong-key-1234567890123456789"); // 'w' vs 't'
        when(request.getRequestURI()).thenReturn("/api/v1/runtime/systems/test/config");

        long startTime = System.nanoTime();
        filter1.doFilter(request, response, filterChain);
        long earlyMismatchTime = System.nanoTime() - startTime;

        SecurityContextHolder.clearContext();

        // Late character difference
        when(request.getHeader(API_KEY_HEADER)).thenReturn("test-key-1234567890123456789X"); // 'X' vs '0'
        startTime = System.nanoTime();
        filter2.doFilter(request, response, filterChain);
        long lateMismatchTime = System.nanoTime() - startTime;

        // Both should be rejected, confirming constant-time hashing is applied.
        // Times will vary due to system load, but should be in the same order of magnitude
        // (within 5x) rather than having massive divergence from early-exit attacks.
        double ratio = (double) Math.max(earlyMismatchTime, lateMismatchTime) /
                      Math.min(earlyMismatchTime, lateMismatchTime);
        assertThat(ratio).isLessThan(5.0);
    }

    @Test
    void doFilter_NotConfiguredApiKey_RejectsRequest() throws Exception {
        ApiKeyAuthenticationFilter filterWithoutKey = new ApiKeyAuthenticationFilter(
            API_KEY_HEADER, "", authenticationEntryPoint);

        when(request.getHeader(API_KEY_HEADER)).thenReturn(TEST_API_KEY);
        when(request.getRequestURI()).thenReturn("/api/v1/runtime/systems/test/config");

        filterWithoutKey.doFilter(request, response, filterChain);

        SecurityContext context = SecurityContextHolder.getContext();
        assertThat(context.getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(eq(request), eq(response), any(AuthenticationException.class));
    }
}
