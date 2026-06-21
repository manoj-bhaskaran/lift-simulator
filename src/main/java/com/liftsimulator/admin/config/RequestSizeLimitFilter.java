package com.liftsimulator.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Rejects oversized API request bodies before JSON deserialization can allocate unbounded memory.
 */
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final RequestSizeLimitProperties properties;

    public RequestSizeLimitFilter(RequestSizeLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled()
            || properties.getMaxBodyBytes() <= 0
            || (!request.getRequestURI().startsWith("/api/v1/")
                && !request.getRequestURI().startsWith("/actuator/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long maxBodyBytes = properties.getMaxBodyBytes();
        long contentLength = request.getContentLengthLong();
        if (contentLength > maxBodyBytes) {
            writePayloadTooLarge(response, maxBodyBytes);
            return;
        }

        try {
            filterChain.doFilter(new LimitedBodyRequest(request, maxBodyBytes), response);
        } catch (PayloadTooLargeException e) {
            writePayloadTooLarge(response, maxBodyBytes);
        }
    }

    private void writePayloadTooLarge(
            HttpServletResponse response,
            long maxBodyBytes) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(String.format(
            "{\"status\":413,\"error\":\"Payload Too Large\","
                + "\"message\":\"Request body exceeds the %d byte limit.\"}",
            maxBodyBytes));
    }

    private static final class LimitedBodyRequest extends HttpServletRequestWrapper {
        private final long maxBodyBytes;

        private LimitedBodyRequest(HttpServletRequest request, long maxBodyBytes) {
            super(request);
            this.maxBodyBytes = maxBodyBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new LimitedServletInputStream(super.getInputStream(), maxBodyBytes);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(
                new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)
            );
        }
    }

    private static final class LimitedServletInputStream extends ServletInputStream {
        private final ServletInputStream delegate;
        private final long maxBodyBytes;
        private long bytesRead;

        private LimitedServletInputStream(ServletInputStream delegate, long maxBodyBytes) {
            this.delegate = delegate;
            this.maxBodyBytes = maxBodyBytes;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value != -1) {
                incrementAndCheck(1);
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = delegate.read(b, off, len);
            if (read > 0) {
                incrementAndCheck(read);
            }
            return read;
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            delegate.setReadListener(readListener);
        }

        private void incrementAndCheck(int count) throws IOException {
            bytesRead += count;
            if (bytesRead > maxBodyBytes) {
                throw new PayloadTooLargeException();
            }
        }
    }

    private static final class PayloadTooLargeException extends IOException {
    }
}
