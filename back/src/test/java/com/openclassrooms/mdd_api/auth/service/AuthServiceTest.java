package com.openclassrooms.mdd_api.auth.service;

import com.openclassrooms.mdd_api.auth.dto.LoginRequest;
import com.openclassrooms.mdd_api.auth.dto.RegisterRequest;
import com.openclassrooms.mdd_api.auth.dto.TokenResponse;
import com.openclassrooms.mdd_api.common.web.exception.ApiBadRequestException;
import com.openclassrooms.mdd_api.common.web.exception.ApiConflictException;
import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 * SUT (System Under Test):
 *  - AuthService: register/login/refresh/logout business logic
 * Scope:
 *  - Pure unit tests (no Spring context, no DB, no filesystem, no network)
 * Design choices:
 *  - Dependencies mocked: UserRepository, PasswordEncoder, JwtService, RefreshTokenService.
 *  - Focus on behavior: exceptions, normalization, and key interactions.
 * How to run:
 *  - Windows: .\mvnw.cmd -Dtest=AuthServiceTest test
 *  - Full checks (incl. JaCoCo): .\mvnw.cmd verify
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;

    @InjectMocks AuthService authService;

    // ---------------- register ----------------

    @Test
    @DisplayName("register: rejects password not matching policy with 400")
    void register_rejectsInvalidPasswordPolicy() {
        // Arrange
        RegisterRequest req = mock(RegisterRequest.class);
        when(req.password()).thenReturn("short"); // invalid => exception before any repo call

        // Act + Assert
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ApiBadRequestException.class)
                .hasMessageContaining("Password policy");

        verifyNoInteractions(userRepository, passwordEncoder, jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("register: normalizes email (trim+lowercase) and username (trim), then saves")
    void register_normalizesAndSaves() {
        // Arrange
        RegisterRequest req = mock(RegisterRequest.class);
        when(req.email()).thenReturn("  John.Doe@Example.COM  ");
        when(req.username()).thenReturn("  JohnDoe  ");
        when(req.password()).thenReturn("Abcdef1!"); // expected valid

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("JohnDoe")).thenReturn(false);

        when(passwordEncoder.encode("Abcdef1!")).thenReturn("ENC");

        User saved = mock(User.class);
        when(saved.getId()).thenReturn(123L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        // Act
        Long id = authService.register(req);

        // Assert
        assertThat(id).isEqualTo(123L);
        verify(userRepository).existsByEmail("john.doe@example.com");
        verify(userRepository).existsByUsername("JohnDoe");
        verify(passwordEncoder).encode("Abcdef1!");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue()).isNotNull();
        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("register: throws 409 if email already used")
    void register_conflictWhenEmailExists() {
        // Arrange
        RegisterRequest req = mock(RegisterRequest.class);
        when(req.email()).thenReturn("  A@B.com  ");
        when(req.username()).thenReturn("User");
        when(req.password()).thenReturn("Abcdef1!");

        when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("already used");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("register: throws 409 if username already used")
    void register_conflictWhenUsernameExists() {
        // Arrange
        RegisterRequest req = mock(RegisterRequest.class);
        when(req.email()).thenReturn("x@y.com");
        when(req.username()).thenReturn("  Taken  ");
        when(req.password()).thenReturn("Abcdef1!");

        when(userRepository.existsByEmail("x@y.com")).thenReturn(false);
        when(userRepository.existsByUsername("Taken")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("already used");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("register: maps DataIntegrityViolationException to 409 (anti-race)")
    void register_conflictWhenDataIntegrityViolation() {
        // Arrange
        RegisterRequest req = mock(RegisterRequest.class);
        when(req.email()).thenReturn("x@y.com");
        when(req.username()).thenReturn("User");
        when(req.password()).thenReturn("Abcdef1!");

        when(userRepository.existsByEmail("x@y.com")).thenReturn(false);
        when(userRepository.existsByUsername("User")).thenReturn(false);

        when(passwordEncoder.encode(anyString())).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));

        // Act + Assert
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("already used");

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    // ---------------- login ----------------

    @Test
    @DisplayName("login: invalid credentials when neither email nor username found")
    void login_invalidWhenNoUserFound() {
        // Arrange
        LoginRequest req = mock(LoginRequest.class);
        when(req.identifier()).thenReturn("  JOHN  ");

        when(userRepository.findByEmail("john")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("JOHN")).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("login: invalid credentials when password does not match")
    void login_invalidWhenPasswordMismatch() {
        // Arrange
        LoginRequest req = mock(LoginRequest.class);
        when(req.identifier()).thenReturn("user@example.com");
        when(req.password()).thenReturn("bad-pass");

        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn("HASH");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad-pass", "HASH")).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("login: issues access + refresh token on happy path (email lookup)")
    void login_happyPath_email() {
        // Arrange
        LoginRequest req = mock(LoginRequest.class);
        when(req.identifier()).thenReturn("  USER@Example.COM  ");
        when(req.password()).thenReturn("Abcdef1!");

        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn("HASH");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Abcdef1!", "HASH")).thenReturn(true);

        TokenResponse access = mock(TokenResponse.class);
        when(jwtService.issueAccessToken(user)).thenReturn(access);

        var issued = new RefreshTokenService.Issued("REFRESH_RAW", Instant.EPOCH);
        when(refreshTokenService.issueSingleSession(user)).thenReturn(issued);

        // Act
        AuthService.TokenBundle bundle = authService.login(req);

        // Assert
        assertThat(bundle.tokenResponse()).isSameAs(access);
        assertThat(bundle.refreshTokenRaw()).isEqualTo("REFRESH_RAW");

        verify(userRepository).findByEmail("user@example.com");
        verify(jwtService).issueAccessToken(user);
        verify(refreshTokenService).issueSingleSession(user);
    }

    @Test
    @DisplayName("login: falls back to username lookup when email lookup misses")
    void login_fallbackToUsername() {
        // Arrange
        LoginRequest req = mock(LoginRequest.class);
        when(req.identifier()).thenReturn("  JohnDoe  ");
        when(req.password()).thenReturn("Abcdef1!");

        when(userRepository.findByEmail("johndoe")).thenReturn(Optional.empty());

        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn("HASH");
        when(userRepository.findByUsername("JohnDoe")).thenReturn(Optional.of(user));

        when(passwordEncoder.matches("Abcdef1!", "HASH")).thenReturn(true);

        TokenResponse access = mock(TokenResponse.class);
        when(jwtService.issueAccessToken(user)).thenReturn(access);

        var issued = new RefreshTokenService.Issued("REFRESH_RAW", Instant.EPOCH);
        when(refreshTokenService.issueSingleSession(user)).thenReturn(issued);

        // Act
        AuthService.TokenBundle bundle = authService.login(req);

        // Assert
        assertThat(bundle.tokenResponse()).isSameAs(access);
        assertThat(bundle.refreshTokenRaw()).isEqualTo("REFRESH_RAW");

        verify(userRepository).findByUsername("JohnDoe");
    }

    // ---------------- refresh ----------------

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("refresh: rejects missing/blank refresh token with 401")
    void refresh_rejectsMissingToken(String token) {
        // Act + Assert
        assertThatThrownBy(() -> authService.refresh(token))
                .isInstanceOf(ApiUnauthorizedException.class)
                .hasMessageContaining("Missing refresh token");

        verifyNoInteractions(refreshTokenService, jwtService, userRepository);
    }

    @Test
    @DisplayName("refresh: propagates invalid refresh token when rotation fails")
    void refresh_propagatesInvalidWhenRotateThrows() {
        // Arrange
        when(refreshTokenService.rotate("BAD"))
                .thenThrow(new ApiUnauthorizedException("Invalid refresh token"));

        // Act + Assert
        assertThatThrownBy(() -> authService.refresh("BAD"))
                .isInstanceOf(ApiUnauthorizedException.class)
                .hasMessageContaining("Invalid refresh token");

        verifyNoInteractions(userRepository, jwtService);
    }

    @Test
    @DisplayName("refresh: rejects when rotated token user not found")
    void refresh_rejectsWhenUserNotFound() {
        // Arrange
        var rotated = new RefreshTokenService.Rotated(
                99L,
                new RefreshTokenService.Issued("NEW_REFRESH", Instant.EPOCH)
        );

        when(refreshTokenService.rotate("OLD")).thenReturn(rotated);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> authService.refresh("OLD"))
                .isInstanceOf(ApiUnauthorizedException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(jwtService, never()).issueAccessToken(any());
    }

    @Test
    @DisplayName("refresh: returns new access token and rotated refresh token on happy path")
    void refresh_happyPath() {
        // Arrange
        var rotated = new RefreshTokenService.Rotated(
                7L,
                new RefreshTokenService.Issued("NEW_REFRESH", Instant.EPOCH)
        );
        when(refreshTokenService.rotate("OLD")).thenReturn(rotated);

        User user = mock(User.class);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        TokenResponse access = mock(TokenResponse.class);
        when(jwtService.issueAccessToken(user)).thenReturn(access);

        // Act
        AuthService.TokenBundle bundle = authService.refresh("OLD");

        // Assert
        assertThat(bundle.tokenResponse()).isSameAs(access);
        assertThat(bundle.refreshTokenRaw()).isEqualTo("NEW_REFRESH");
    }

    // ---------------- logout ----------------

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("logout: rejects missing/blank refresh token with 401")
    void logout_rejectsMissingToken(String token) {
        // Act + Assert
        assertThatThrownBy(() -> authService.logout(token))
                .isInstanceOf(ApiUnauthorizedException.class)
                .hasMessageContaining("Missing refresh token");

        verify(refreshTokenService, never()).revoke(anyString());
        verifyNoInteractions(userRepository, jwtService);
    }

    @Test
    @DisplayName("logout: revokes refresh token when provided")
    void logout_revokesToken() {
        // Arrange
        String token = "REFRESH";

        // Act
        authService.logout(token);

        // Assert
        verify(refreshTokenService).revoke("REFRESH");
        verifyNoInteractions(userRepository, jwtService);
    }
}
