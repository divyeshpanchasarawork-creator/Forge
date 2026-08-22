package com.forge.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ProdSecurityGuardTest {

    private ProdSecurityGuard guardFor(String jwtSecret) {
        return new ProdSecurityGuard(jwtSecret);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankSecretRefusesToStart(String jwtSecret) {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> guardFor(jwtSecret).run(null));

        assertTrue(ex.getMessage().contains("jwt.secret is not set"));
    }

    @Test
    void knownDevSecretRefusesToStart() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> guardFor(ProdSecurityGuard.KNOWN_DEV_SECRET).run(null));

        assertTrue(ex.getMessage().contains("known dev/test value"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"!!!not-base64!!!", "plain-secret-value"})
    void nonBase64SecretRefusesToStart(String jwtSecret) {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> guardFor(jwtSecret).run(null));

        assertTrue(ex.getMessage().contains("Base64"));
    }

    @Test
    void shortKeyRefusesToStart() {
        String shortKey = Base64.getEncoder().encodeToString("too-short".getBytes());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> guardFor(shortKey).run(null));

        assertTrue(ex.getMessage().contains("at least 32 bytes"));
    }

    @Test
    void validHs256KeyPassesValidation() {
        String validKey = Base64.getEncoder().encodeToString(new byte[32]);

        assertDoesNotThrow(() -> guardFor(validKey).run(null));
    }
}
