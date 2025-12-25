package com.openclassrooms.mdd_api.auth.service;

import com.openclassrooms.mdd_api.auth.entity.RefreshToken;
import com.openclassrooms.mdd_api.auth.repository.RefreshTokenRepository;
import com.openclassrooms.mdd_api.common.config.OcAppProperties;
import com.openclassrooms.mdd_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    public record Issued(String rawToken, Instant expiresAt) {}
    public record Rotated(User user, Issued issued) {}

    private final RefreshTokenRepository refreshTokenRepository;
    private final OcAppProperties props;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public Issued issueSingleSession(User user) {
        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), Instant.now());

        String raw = generateRawToken();
        String hash = sha256Hex(raw);

        Instant expiresAt = Instant.now().plusMillis(props.getRefreshTokenExpirationMs());
        refreshTokenRepository.save(new RefreshToken(user, hash, expiresAt));

        return new Issued(raw, expiresAt);
    }

    @Transactional
    public Rotated rotate(String rawToken) {
        String hash = sha256Hex(rawToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .filter(rt -> rt.isActive(Instant.now()))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        refreshTokenRepository.revokeByTokenHash(hash, Instant.now());

        Issued issued = issueSingleSession(existing.getUser());
        return new Rotated(existing.getUser(), issued);
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokenRepository.revokeByTokenHash(sha256Hex(rawToken), Instant.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot hash token");
        }
    }
}
