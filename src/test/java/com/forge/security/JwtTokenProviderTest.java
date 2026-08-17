package com.forge.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "Zm9yZ2UtdGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHk=";

    private JwtTokenProvider provider;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(TEST_SECRET, 1_800_000L, 604_800_000L);
        principal = new UserPrincipal(UUID.randomUUID(), "testuser", "password", "ADMIN");
    }

    @Test
    void accessTokenRoundTripsToUserId() {
        String token = provider.generateToken(principal);

        assertEquals(principal.getId(), provider.getAccessTokenUserId(token));
    }

    @Test
    void refreshTokenIsRejectedAsAccessToken() {
        String token = provider.generateRefreshToken(principal);

        assertNull(provider.getAccessTokenUserId(token));
        assertTrue(provider.validateRefreshToken(token));
    }

    @Test
    void accessTokenIsRejectedAsRefreshToken() {
        String token = provider.generateToken(principal);

        assertFalse(provider.validateRefreshToken(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = provider.generateToken(principal);
        String tampered = token.substring(0, token.length() - 3) + "xyz";

        assertNull(provider.getAccessTokenUserId(tampered));
        assertFalse(provider.validateRefreshToken(tampered));
    }

    @Test
    void expiredAccessTokenIsRejected() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        String expired = Jwts.builder()
                .subject(principal.getId().toString())
                .claim("username", principal.getUsername())
                .claim("type", "access")
                .issuedAt(new Date(Instant.now().minus(2, ChronoUnit.HOURS).toEpochMilli()))
                .expiration(new Date(Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli()))
                .signWith(key)
                .compact();

        assertNull(provider.getAccessTokenUserId(expired));
    }

    @Test
    void garbageOrBlankTokenIsRejected() {
        assertNull(provider.getAccessTokenUserId(null));
        assertNull(provider.getAccessTokenUserId(""));
        assertNull(provider.getAccessTokenUserId("not-a-jwt"));
    }

    @Test
    void exposesConfiguredRefreshExpiration() {
        assertEquals(604_800_000L, provider.getRefreshExpirationMs());
    }
}
