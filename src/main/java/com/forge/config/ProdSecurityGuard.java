package com.forge.config;

import io.jsonwebtoken.io.Decoders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fails prod startup fast when the JWT signing secret is missing, is the known
 * dev/test value, or cannot form an HS256 key. With lazy-initialization enabled,
 * JwtTokenProvider is created lazily, so an invalid secret would otherwise only
 * surface on the first login attempt.
 */
@Slf4j
@Component
@Profile("prod")
public class ProdSecurityGuard implements ApplicationRunner {

    static final String KNOWN_DEV_SECRET = "Zm9yZ2Utc2VjcmV0LWtleS1mb3ItZGV2LWVudmlyb25tZW50LW9ubHk=";
    private static final int MIN_KEY_BYTES = 32;

    private final String jwtSecret;

    public ProdSecurityGuard(@Value("${jwt.secret:}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (jwtSecret.isBlank()) {
            throw new IllegalStateException("jwt.secret is not set; refusing to start prod without a signing key");
        }
        if (KNOWN_DEV_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("jwt.secret must not use the known dev/test value in prod");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("jwt.secret must be Base64-encoded", e);
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("jwt.secret must decode to at least 32 bytes (HS256)");
        }
        log.info("Prod JWT secret validated ({} bytes)", keyBytes.length);
    }
}
