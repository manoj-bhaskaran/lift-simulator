package com.liftsimulator.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link RateLimitingFilter}.
 * Verifies that the token-bucket rate limiter enforces configured limits,
 * returns HTTP 429 with appropriate headers when exhausted, and is skipped
 * when disabled or for non-API/non-actuator paths.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitingTest {

    @Mock
    private FilterChain filterChain;

    private RateLimitingProperties properties;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RateLimitingProperties();
        // Use a small capacity so tests can exhaust the bucket quickly
        properties.setAdmin(new RateLimitingProperties.EndpointLimits(2, 2, 60));
        properties.setRuntime(new RateLimitingProperties.EndpointLimits(3, 3, 60));
        filter = new RateLimitingFilter(properties);
    }

    @Test
    void doFilter_WithinLimit_AllowsRequest() throws Exception {
        MockHttpServletRequest request = buildRequest("/api/v1/lift-systems", "127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ExceedsAdminLimit_Returns429() throws Exception {
        MockHttpServletResponse lastResponse = null;

        // Exhaust the bucket (capacity = 2) then make one more request
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = buildRequest("/api/v1/lift-systems", "10.0.0.1");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, filterChain);
        }

        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(lastResponse.getHeader("Retry-After")).isNotNull();
        assertThat(lastResponse.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
    }

    @Test
    void doFilter_ExceedsRuntimeLimit_Returns429() throws Exception {
        MockHttpServletResponse lastResponse = null;

        // Exhaust runtime bucket (capacity = 3) then make one more request
        for (int i = 0; i < 4; i++) {
            MockHttpServletRequest request = buildRequest("/api/v1/runtime/systems/test/config", "10.0.0.2");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, filterChain);
        }

        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void doFilter_Returns429_WithJsonBody() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = buildRequest("/api/v1/lift-systems", "10.0.0.3");
            filter.doFilter(request, new MockHttpServletResponse(), filterChain);
        }

        MockHttpServletRequest request = buildRequest("/api/v1/lift-systems", "10.0.0.3");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        String body = response.getContentAsString();
        assertThat(body).contains("\"status\":429");
        assertThat(body).contains("\"error\":\"Too Many Requests\"");
        assertThat(body).contains("retryAfterSeconds");
    }

    @Test
    void doFilter_SetsRateLimitHeaders_OnAllowedRequests() throws Exception {
        MockHttpServletRequest request = buildRequest("/api/v1/lift-systems", "10.0.0.4");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("1");
    }

    @Test
    void doFilter_IsolatesBucketPerIp() throws Exception {
        // IP A exhausts the admin bucket
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = buildRequest("/api/v1/lift-systems", "192.168.1.1");
            filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        // IP B should still be allowed
        MockHttpServletRequest requestB = buildRequest("/api/v1/lift-systems", "192.168.1.2");
        MockHttpServletResponse responseB = new MockHttpServletResponse();
        filter.doFilter(requestB, responseB, filterChain);

        assertThat(responseB.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void doFilter_Disabled_SkipsRateLimiting() throws Exception {
        properties.setEnabled(false);

        MockHttpServletRequest request = buildRequest("/api/v1/lift-systems", "10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_NonApiOrActuatorPath_SkipsRateLimiting() throws Exception {
        MockHttpServletRequest request = buildRequest("/", "10.0.0.6");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }


    @Test
    void doFilter_ActuatorPathUsesAdminBucket() throws Exception {
        MockHttpServletResponse lastResponse = null;

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = buildRequest("/actuator/health", "10.0.0.7");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, filterChain);
        }

        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(lastResponse.getHeader("X-RateLimit-Limit")).isEqualTo("2");
    }

    @Test
    void doFilter_XForwardedFor_IgnoredByDefault() throws Exception {
        // Two different X-Forwarded-For values from the same remoteAddr should share a bucket
        // because trust-forwarded-for=false (default) and remoteAddr is "127.0.0.1" for both
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = buildRequest("/api/v1/lift-systems", "127.0.0.9");
            req.addHeader("X-Forwarded-For", "spoofed-ip-" + i);
            filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }
        // Bucket (capacity=2) should now be exhausted because remoteAddr was reused
        MockHttpServletRequest req = buildRequest("/api/v1/lift-systems", "127.0.0.9");
        req.addHeader("X-Forwarded-For", "spoofed-ip-9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(req, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void doFilter_XForwardedFor_HonouredWhenTrustEnabled() throws Exception {
        properties.setTrustForwardedFor(true);

        MockHttpServletRequest request = buildRequest("/api/v1/lift-systems", "10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_SimulationRunsUseRuntimeBucket() throws Exception {
        // simulation-runs paths (with subresource) should use the runtime bucket (capacity = 3)
        MockHttpServletResponse lastResponse = null;
        for (int i = 0; i < 4; i++) {
            MockHttpServletRequest request = buildRequest("/api/v1/simulation-runs/123", "10.0.1.1");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, filterChain);
        }

        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void doFilter_SimulationRunsBasePathUsesRuntimeBucket() throws Exception {
        // /api/v1/simulation-runs (no trailing slash) must also use the runtime bucket
        MockHttpServletResponse lastResponse = null;
        for (int i = 0; i < 4; i++) {
            MockHttpServletRequest request = buildRequest("/api/v1/simulation-runs", "10.0.1.2");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, filterChain);
        }

        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void doFilter_AdminAndRuntimeBuckets_AreIndependent() throws Exception {
        // Exhaust admin bucket for this IP
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = buildRequest("/api/v1/lift-systems", "10.0.2.1");
            filter.doFilter(req, new MockHttpServletResponse(), filterChain);
        }

        // Runtime bucket for the same IP should still have capacity
        MockHttpServletRequest runtimeReq = buildRequest("/api/v1/runtime/systems/s/config", "10.0.2.1");
        MockHttpServletResponse runtimeResp = new MockHttpServletResponse();
        filter.doFilter(runtimeReq, runtimeResp, filterChain);

        assertThat(runtimeResp.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    private MockHttpServletRequest buildRequest(String uri, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
