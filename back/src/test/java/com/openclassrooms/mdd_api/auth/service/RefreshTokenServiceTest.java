package com.openclassrooms.mdd_api.auth.service;

import com.openclassrooms.mdd_api.auth.entity.RefreshToken;
import com.openclassrooms.mdd_api.auth.repository.RefreshTokenRepository;
import com.openclassrooms.mdd_api.common.config.OcAppProperties;
import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
import com.openclassrooms.mdd_api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RefreshTokenService}.
 * SUT:
 *  - RefreshTokenService: issue/rotate/revoke refresh tokens (single-session)
 * Scope:
 *  - Pure unit tests (no Spring context, no DB)
 *  - Deterministic assertions:
 *      - we don't assert random token value, only its format
 *      - we capture the "now" Instant passed to repository calls to assert expiresAt precisely
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock OcAppProperties props;

    @InjectMocks RefreshTokenService refreshTokenService;

    // -------- issueSingleSession --------

    @Test
    @DisplayName("issueSingleSession: revokes active tokens then saves a new token and returns Issued(raw, expiresAt)")
    void issueSingleSession_revokesThenPersistsAndReturnsIssued() {
        // Arrange
        long expMs = 120_000L;
        when(props.getRefreshTokenExpirationMs()).thenReturn(expMs);

        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);

        // Act
        RefreshTokenService.Issued issued = refreshTokenService.issueSingleSession(user);

        // Assert (token format: Base64URL no padding for 32 bytes => 43 chars, no '=')
        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.rawToken()).doesNotContain("=");
        assertThat(issued.rawToken()).hasSize(43);
        assertThat(issued.rawToken()).matches("^[A-Za-z0-9_-]+$");

        // Capture "now" used internally to assert expiresAt = now + expMs exactly
        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).revokeAllActiveByUserId(eq(42L), nowCaptor.capture());

        Instant nowUsed = nowCaptor.getValue();
        assertThat(issued.expiresAt()).isEqualTo(nowUsed.plusMillis(expMs));

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    // -------- rotate --------

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("rotate: rejects missing/blank token with 401")
    void rotate_rejectsMissingOrBlank(String token) {
        // Act + Assert
        assertThatThrownBy(() -> refreshTokenService.rotate(token))
                .isInstanceOf(ApiUnauthorizedException.class)
                .hasMessageContaining("Missing refresh token");

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("rotate: rejects invalid token when not found")
    void rotate_rejectsWhenNotFound() {
        // Arrange
        String raw = "RAW_TOKEN";
        String expectedHash = sha256Hex(raw);

        when(refreshTokenRepository.findByTokenHashForUpdate(expectedHash))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> refreshTokenService.rotate(raw))
                .isInstanceOf(ApiUnauthorizedException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(refreshTokenRepository).findByTokenHashForUpdate(expectedHash);
        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(anyLong(), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("rotate: rejects invalid token when found but inactive")
    void rotate_rejectsWhenInactive() {
        // Arrange
        String raw = "RAW_TOKEN";
        String expectedHash = sha256Hex(raw);

        RefreshToken existing = mock(RefreshToken.class);
        when(existing.isActive(any(Instant.class))).thenReturn(false);

        when(refreshTokenRepository.findByTokenHashForUpdate(expectedHash))
                .thenReturn(Optional.of(existing));

        // Act + Assert
        assertThatThrownBy(() -> refreshTokenService.rotate(raw))
                .isInstanceOf(ApiUnauthorizedException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(refreshTokenRepository).findByTokenHashForUpdate(expectedHash);
        verify(existing).isActive(any(Instant.class));
        verify(refreshTokenRepository, never()).revokeAllActiveByUserId(anyLong(), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("rotate: revokes all active tokens for user, persists a new one, and returns Rotated(userId, issued)")
    void rotate_happyPath() {
        // Arrange
        long expMs = 90_000L;
        when(props.getRefreshTokenExpirationMs()).thenReturn(expMs);

        String raw = "RAW_TOKEN";
        String expectedHash = sha256Hex(raw);

        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);

        RefreshToken existing = mock(RefreshToken.class);
        when(existing.isActive(any(Instant.class))).thenReturn(true);
        when(existing.getUser()).thenReturn(user);

        when(refreshTokenRepository.findByTokenHashForUpdate(expectedHash))
                .thenReturn(Optional.of(existing));

        // Act
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(raw);

        // Assert
        assertThat(rotated.userId()).isEqualTo(7L);

        // Issued token format
        assertThat(rotated.issued().rawToken()).isNotBlank();
        assertThat(rotated.issued().rawToken()).doesNotContain("=");
        assertThat(rotated.issued().rawToken()).hasSize(43);
        assertThat(rotated.issued().rawToken()).matches("^[A-Za-z0-9_-]+$");

        // verify hash usage + interactions
        verify(refreshTokenRepository).findByTokenHashForUpdate(expectedHash);
        verify(existing).isActive(any(Instant.class));

        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).revokeAllActiveByUserId(eq(7L), nowCaptor.capture());

        Instant nowUsed = nowCaptor.getValue();
        assertThat(rotated.issued().expiresAt()).isEqualTo(nowUsed.plusMillis(expMs));

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    // -------- revoke --------

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("revoke: no-op when token missing/blank")
    void revoke_noopWhenMissingOrBlank(String token) {
        // Act
        refreshTokenService.revoke(token);

        // Assert
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    @DisplayName("revoke: hashes token and calls repository with current instant")
    void revoke_hashesAndCallsRepository() {
        // Arrange
        String raw = "RAW_TOKEN";
        String expectedHash = sha256Hex(raw);

        Instant before = Instant.now();

        // Act
        refreshTokenService.revoke(raw);

        Instant after = Instant.now();

        // Assert
        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(refreshTokenRepository).revokeByTokenHash(eq(expectedHash), nowCaptor.capture());

        Instant used = nowCaptor.getValue();
        assertThat(used)
                .describedAs("Instant used for revokeByTokenHash should be between before and after")
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    // ---- helper: same hashing as service, but in test (deterministic) ----
    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
