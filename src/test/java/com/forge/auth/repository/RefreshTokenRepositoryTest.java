package com.forge.auth.repository;

import com.forge.auth.entity.RefreshToken;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:refresh-repo-test;DB_CLOSE_DELAY=-1")
@Transactional
class RefreshTokenRepositoryTest {

    @Autowired private RefreshTokenRepository repository;
    @Autowired private EntityManager entityManager;

    private RefreshToken token(UUID userId, LocalDateTime expiresAt, boolean revoked) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(UUID.randomUUID().toString().replace("-", ""));
        token.setExpiresAt(expiresAt);
        token.setRevoked(revoked);
        return repository.saveAndFlush(token);
    }

    @Test
    void markRevokedClaimsAnActiveTokenExactlyOnce() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = token(userId, LocalDateTime.now().plusDays(7), false);

        assertEquals(1, repository.markRevoked(token.getId(), LocalDateTime.now()));
        entityManager.clear();
        assertTrue(repository.findById(token.getId()).orElseThrow().isRevoked());

        assertEquals(0, repository.markRevoked(token.getId(), LocalDateTime.now()));
    }

    @Test
    void markRevokedSkipsExpiredToken() {
        RefreshToken token = token(UUID.randomUUID(), LocalDateTime.now().minusDays(1), false);

        assertEquals(0, repository.markRevoked(token.getId(), LocalDateTime.now()));
        entityManager.clear();
        assertFalse(repository.findById(token.getId()).orElseThrow().isRevoked());
    }

    @Test
    void markRevokedRequiresMatchingUserIdAndCurrentTimestamp() {
        RefreshToken token = token(UUID.randomUUID(), LocalDateTime.now().plusDays(7), false);
        LocalDateTime now = LocalDateTime.now();

        assertEquals(0, repository.markRevoked(UUID.randomUUID(), now));
        assertEquals(0, repository.markRevoked(token.getId(), now.plusDays(8)));
    }

    @Test
    void revokeAllForUserRevokesOnlyActiveTokensOfThatUser() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        RefreshToken active = token(owner, LocalDateTime.now().plusDays(7), false);
        RefreshToken expired = token(owner, LocalDateTime.now().minusDays(1), false);
        RefreshToken alreadyRevoked = token(owner, LocalDateTime.now().plusDays(7), true);
        RefreshToken otherUserToken = token(other, LocalDateTime.now().plusDays(7), false);

        assertEquals(2, repository.revokeAllForUser(owner));
        entityManager.clear();
        assertTrue(repository.findById(active.getId()).orElseThrow().isRevoked());
        assertTrue(repository.findById(expired.getId()).orElseThrow().isRevoked());
        assertTrue(repository.findById(alreadyRevoked.getId()).orElseThrow().isRevoked());
        assertFalse(repository.findById(otherUserToken.getId()).orElseThrow().isRevoked());
    }

    @Test
    void findByTokenHashAndRevokedFalseAndExpiresAtAfterOnlyMatchesLiveTokens() {
        UUID userId = UUID.randomUUID();
        RefreshToken live = token(userId, LocalDateTime.now().plusDays(7), false);
        token(userId, LocalDateTime.now().plusDays(7), true);
        token(userId, LocalDateTime.now().minusDays(1), false);
        LocalDateTime now = LocalDateTime.now();

        assertEquals(live.getId(), repository
                .findByTokenHashAndRevokedFalseAndExpiresAtAfter(live.getTokenHash(), now)
                .orElseThrow().getId());
        assertTrue(repository.findByTokenHashAndRevokedFalseAndExpiresAtAfter(
                UUID.randomUUID().toString().replace("-", ""), now).isEmpty());
    }
}
