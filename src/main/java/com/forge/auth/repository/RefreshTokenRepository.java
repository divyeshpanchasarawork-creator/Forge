package com.forge.auth.repository;

import com.forge.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByTokenHashAndRevokedFalseAndExpiresAtAfter(String tokenHash, LocalDateTime now);

    void deleteByUserId(UUID userId);

    /**
     * Atomically claims a refresh token for rotation: sets revoked = true only if the token
     * is still valid and unrevoked. Concurrent refreshes with the same token race here — the
     * losing transaction matches 0 rows and is rejected, so a stolen token cannot mint two
     * live sessions.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true " +
            "WHERE rt.id = :id AND rt.revoked = false AND rt.expiresAt > :now")
    int markRevoked(@Param("id") UUID id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId AND rt.revoked = false")
    int revokeAllForUser(@Param("userId") UUID userId);
}
