package com.liftsimulator.admin.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servlet filter that enforces API rate limits using a token bucket algorithm (Bucket4j).
 *
 * <p>Separate buckets are maintained per client IP address for admin and runtime API paths.
 * When a bucket is exhausted the filter returns HTTP 429 with a {@code Retry-After} header
 * indicating how many seconds the client must wait before retrying.
 *
 * <p>Rate limits are configurable via {@link RateLimitingProperties}:
 * <ul>
 *   <li>{@code rate-limiting.admin.*}  — admin API paths ({@code /api/v1/**} excluding runtime)</li>
 *   <li>{@code rate-limiting.runtime.*} — runtime and simulation-run paths</li>
 * </ul>
 *
 * <p>Set {@code rate-limiting.enabled=false} to disable rate limiting entirely (e.g., in tests).
 * Set {@code rate-limiting.trust-forwarded-for=true} only when the service sits behind a
 * trusted reverse proxy that controls the {@code X-Forwarded-For} header; leaving it false
 * (the default) uses {@code getRemoteAddr()} and prevents bucket-key spoofing.
 *
 * <p>Registered via {@link RateLimitingConfig} as a {@code FilterRegistrationBean} at order
 * {@code SecurityProperties.DEFAULT_FILTER_ORDER - 1} so it runs before the Spring Security
 * filter chain and can throttle unauthenticated requests as well as authenticated ones.
 *
 * @see RateLimitingProperties
 * @see RateLimitingConfig
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final String RUNTIME_PREFIX = "/api/v1/runtime/";
    private static final String SIMULATION_RUNS_BASE = "/api/v1/simulation-runs";
    private static final String SIMULATION_RUNS_PREFIX = SIMULATION_RUNS_BASE + "/";
    private static final String HEADER_RETRY_AFTER = "Retry-After";
    private static final String HEADER_X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_X_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";

    private final RateLimitingProperties properties;
    private final ConcurrentHashMap<String, Bucket> adminBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> runtimeBuckets = new ConcurrentHashMap<>();

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "RateLimitingProperties is a Spring-managed singleton bound via "
                    + "@ConfigurationProperties. Defensive copying would break the binding.")
    public RateLimitingFilter(RateLimitingProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        String uri = request.getRequestURI();
        boolean isRuntime = uri.startsWith(RUNTIME_PREFIX)
            || uri.equals(SIMULATION_RUNS_BASE)
            || uri.startsWith(SIMULATION_RUNS_PREFIX);

        Bucket bucket;
        int limit;
        if (isRuntime) {
            RateLimitingProperties.EndpointLimits runtimeLimits = properties.getRuntime();
            bucket = runtimeBuckets.computeIfAbsent(clientIp, k -> buildBucket(runtimeLimits));
            limit = runtimeLimits.getCapacity();
        } else {
            RateLimitingProperties.EndpointLimits adminLimits = properties.getAdmin();
            bucket = adminBuckets.computeIfAbsent(clientIp, k -> buildBucket(adminLimits));
            limit = adminLimits.getCapacity();
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        response.setHeader(HEADER_X_RATE_LIMIT_LIMIT, String.valueOf(limit));

        if (probe.isConsumed()) {
            response.setHeader(HEADER_X_RATE_LIMIT_REMAINING, String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L;
            logger.warn("Rate limit exceeded for IP {} on {}", clientIp, uri);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfterSeconds));
            response.setHeader(HEADER_X_RATE_LIMIT_REMAINING, "0");
            response.getWriter().write(String.format(
                "{\"status\":429,\"error\":\"Too Many Requests\","
                + "\"message\":\"Rate limit exceeded. Retry after %d second(s).\","
                + "\"retryAfterSeconds\":%d}",
                retryAfterSeconds, retryAfterSeconds));
        }
    }

    private Bucket buildBucket(RateLimitingProperties.EndpointLimits limits) {
        Bandwidth bandwidth = Bandwidth.builder()
            .capacity(limits.getCapacity())
            .refillGreedy(limits.getRefillTokens(), Duration.ofSeconds(limits.getRefillPeriodSeconds()))
            .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }

    /**
     * Resolves the client IP used as the rate-limit bucket key.
     * Honours {@code X-Forwarded-For} only when {@code rate-limiting.trust-forwarded-for=true}
     * to prevent bucket-key spoofing when the service is reachable directly.
     */
    private String resolveClientIp(HttpServletRequest request) {
        if (properties.isTrustForwardedFor()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int commaIndex = forwarded.indexOf(',');
                return commaIndex > 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
