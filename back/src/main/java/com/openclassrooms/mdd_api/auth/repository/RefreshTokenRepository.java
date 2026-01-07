package com.openclassrooms.mdd_api.auth.repository;

import com.openclassrooms.mdd_api.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Verrouille la ligne du refresh token pour éviter la double-consommation lors d'un refresh concurrent.
     * (Rotation : une seule requête doit pouvoir consommer un token donné.)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select rt from RefreshToken rt
        where rt.tokenHash = :tokenHash
    """)
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /**
     * Clear/flush automatiquement pour éviter un état incohérent du persistence context.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RefreshToken rt
        set rt.revokedAt = :now
        where rt.user.id = :userId and rt.revokedAt is null
    """)
    int revokeAllActiveByUserId(@Param("userId") long userId, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RefreshToken rt
        set rt.revokedAt = :now
        where rt.tokenHash = :tokenHash and rt.revokedAt is null
    """)
    int revokeByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") Instant now);
}
