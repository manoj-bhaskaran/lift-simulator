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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Rejects oversized API request bodies before JSON deserialization can allocate unbounded memory.
 */
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private static final int BUFFER_SIZE = 8192;

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

        byte[] body = readBodyWithinLimit(request, maxBodyBytes);
        if (body == null) {
            writePayloadTooLarge(response, maxBodyBytes);
            return;
        }

        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private byte[] readBodyWithinLimit(HttpServletRequest request, long maxBodyBytes) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        long bytesRead = 0;
        int read;
        try (ServletInputStream inputStream = request.getInputStream()) {
            while ((read = inputStream.read(buffer)) != -1) {
                bytesRead += read;
                if (bytesRead > maxBodyBytes) {
                    return null;
                }
                body.write(buffer, 0, read);
            }
        }
        return body.toByteArray();
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

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body.clone();
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedBodyServletInputStream(body);
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(
                new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)
            );
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }

    private static final class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream delegate;

        private CachedBodyServletInputStream(byte[] body) {
            this.delegate = new ByteArrayInputStream(body);
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return delegate.read(b, off, len);
        }

        @Override
        public boolean isFinished() {
            return delegate.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException("Asynchronous reads are not supported");
        }
    }
}
