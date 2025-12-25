package com.openclassrooms.mdd_api.auth.repository;

import com.openclassrooms.mdd_api.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        update RefreshToken rt
        set rt.revokedAt = :now
        where rt.user.id = :userId and rt.revokedAt is null
    """)
    int revokeAllActiveByUserId(long userId, Instant now);

    @Modifying
    @Query("""
        update RefreshToken rt
        set rt.revokedAt = :now
        where rt.tokenHash = :tokenHash and rt.revokedAt is null
    """)
    int revokeByTokenHash(String tokenHash, Instant now);
}
