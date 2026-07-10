package com.liftsimulator.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestSizeLimitFilterTest {

    @Mock
    private FilterChain filterChain;

    private RequestSizeLimitProperties properties;
    private RequestSizeLimitFilter filter;

    @BeforeEach
    void setUp() {
        properties = new RequestSizeLimitProperties();
        properties.setMaxBodyBytes(8);
        filter = new RequestSizeLimitFilter(properties);
    }

    @Test
    void doFilterRejectsOversizedContentLength() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/lift-systems");
        request.setContent("too-large".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE.value());
        assertThat(response.getContentAsString()).contains("Request body exceeds");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterRejectsOversizedStreamBeforeDispatch() throws Exception {
        MockHttpServletRequest request = new UnknownLengthRequest("POST", "/api/v1/lift-systems");
        request.setContent("too-large".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE.value());
        assertThat(response.getContentAsString()).contains("Request body exceeds");
        verify(filterChain, never()).doFilter(any(), eq(response));
    }

    @Test
    void doFilterAllowsRequestsWithinLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/lift-systems");
        request.setContent("ok".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        doAnswer(invocation -> {
            ServletRequest wrappedRequest = invocation.getArgument(0);
            byte[] body = wrappedRequest.getInputStream().readAllBytes();
            assertThat(new String(body, StandardCharsets.UTF_8)).isEqualTo("ok");
            return null;
        }).when(filterChain).doFilter(any(), eq(response));

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(filterChain).doFilter(any(), eq(response));
    }

    private static final class UnknownLengthRequest extends MockHttpServletRequest {
        private UnknownLengthRequest(String method, String requestUri) {
            super(method, requestUri);
        }

        @Override
        public long getContentLengthLong() {
            return -1;
        }
    }
}
