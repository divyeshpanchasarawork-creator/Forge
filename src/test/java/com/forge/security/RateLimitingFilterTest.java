package com.forge.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
        chain = mock(FilterChain.class);
    }

    private MockHttpServletRequest authRequest(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr(ip);
        return req;
    }

    private int send(String ip, String xff) throws Exception {
        MockHttpServletRequest req = authRequest(ip);
        if (xff != null) {
            req.addHeader("X-Forwarded-For", xff);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        return res.getStatus();
    }

    @Test
    void allowsFiveRequestsThenRejectsWith429() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, send("1.2.3.4", null));
        }
        assertEquals(429, send("1.2.3.4", null));
        verify(chain, times(5)).doFilter(any(), any());
    }

    @Test
    void bucketsAreIsolatedPerIp() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, send("1.1.1.1", null));
        }
        // 1.1.1.1 exhausted, but a fresh IP still has its own full bucket
        assertEquals(429, send("1.1.1.1", null));
        for (int i = 0; i < 5; i++) {
            assertEquals(200, send("2.2.2.2", null));
        }
        assertEquals(429, send("2.2.2.2", null));
        verify(chain, times(10)).doFilter(any(), any());
    }

    @Test
    void nonAuthPathsAreNotRateLimited() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/health");
        req.setRemoteAddr("1.2.3.4");
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus());
        }
        verify(chain, times(20)).doFilter(any(), any());
    }

    @Test
    void keysOnRightmostXffHopNotRemoteAddr() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, send("spoofed-" + i, "attacker, 10.0.0.1"));
        }
        assertEquals(429, send("spoofed-6", "attacker, 10.0.0.1"));
        verify(chain, times(5)).doFilter(any(), any());
    }

    @Test
    void malformedXffCollapsesIntoSharedUnknownBucket() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, send("3.3.3." + i, "not-an-ip!!"));
        }
        assertEquals(429, send("3.3.3.99", "not-an-ip!!"));
        verify(chain, times(5)).doFilter(any(), any());
    }
}
