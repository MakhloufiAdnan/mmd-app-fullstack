package com.openclassrooms.mdd_api.auth.service;

import com.openclassrooms.mdd_api.auth.entity.RefreshToken;
import com.openclassrooms.mdd_api.auth.repository.RefreshTokenRepository;
import com.openclassrooms.mdd_api.common.config.OcAppProperties;
import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
import com.openclassrooms.mdd_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public record Issued(String rawToken, Instant expiresAt) {}

    /**
     * IMPORTANT : on ne renvoie pas l'entité User (LAZY) pour éviter les LazyInitializationException.
     * On renvoie un identifiant stable (userId).
     */
    public record Rotated(long userId, Issued issued) {}

    private static final int RAW_TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final OcAppProperties props;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public Issued issueSingleSession(User user) {
        Instant now = Instant.now();
        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), now);
        return persistNewToken(user, now);
    }

    /**
     * Rotation du refresh token :
     *  consomme le token existant (révocation)
     *  émet un nouveau token
     * La lecture est verrouillée (PESSIMISTIC_WRITE) pour empêcher
     * deux refresh concurrents de consommer le même token.
     */
    @Transactional
    public Rotated rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiUnauthorizedException("Missing refresh token");
        }

        Instant now = Instant.now();
        String hash = sha256Hex(rawToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHashForUpdate(hash)
                .filter(rt -> rt.isActive(now))
                .orElseThrow(() -> new ApiUnauthorizedException("Invalid refresh token"));

        long userId = existing.getUser().getId();

        // Single session : révoque tous les tokens actifs
        refreshTokenRepository.revokeAllActiveByUserId(userId, now);

        // Émet un nouveau refresh token (user proxy OK pour FK)
        Issued issued = persistNewToken(existing.getUser(), now);

        return new Rotated(userId, issued);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokenRepository.revokeByTokenHash(sha256Hex(rawToken), Instant.now());
    }

    private Issued persistNewToken(User user, Instant now) {
        String raw = generateRawToken();
        String hash = sha256Hex(raw);

        Instant expiresAt = now.plusMillis(props.getRefreshTokenExpirationMs());
        refreshTokenRepository.save(new RefreshToken(user, hash, expiresAt));

        return new Issued(raw, expiresAt);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
