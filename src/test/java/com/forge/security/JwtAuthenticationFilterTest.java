package com.forge.security;

import com.forge.auth.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String authorizationHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        return request;
    }

    @Test
    void validAccessTokenAuthenticatesUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "admin", "pw", "ADMIN");
        when(jwtTokenProvider.getAccessTokenUserId("valid.jwt")).thenReturn(userId);
        when(userDetailsService.loadUserById(userId)).thenReturn(principal);

        filter.doFilterInternal(request("Bearer valid.jwt"), new MockHttpServletResponse(), filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertEquals(principal, auth.getPrincipal());
        assertEquals(1, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(auth instanceof UsernamePasswordAuthenticationToken);
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void missingHeaderLeavesContextUnauthenticated() throws Exception {
        filter.doFilterInternal(request(null), new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
        verify(jwtTokenProvider, never()).getAccessTokenUserId(any());
    }

    @Test
    void nonBearerHeaderIsIgnored() throws Exception {
        filter.doFilterInternal(request("Basic dXNlcjpwYXNz"), new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtTokenProvider, never()).getAccessTokenUserId(any());
    }

    @Test
    void invalidTokenLeavesContextUnauthenticated() throws Exception {
        when(jwtTokenProvider.getAccessTokenUserId("expired.jwt")).thenReturn(null);

        filter.doFilterInternal(request("Bearer expired.jwt"), new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserById(any());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void providerExceptionIsSwallowedAndChainProceeds() throws Exception {
        when(jwtTokenProvider.getAccessTokenUserId(any())).thenThrow(new RuntimeException("boom"));

        filter.doFilterInternal(request("Bearer bad.jwt"), new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }
}
