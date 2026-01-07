package com.openclassrooms.mdd_api.auth.service;

import com.openclassrooms.mdd_api.auth.dto.LoginRequest;
import com.openclassrooms.mdd_api.auth.dto.RegisterRequest;
import com.openclassrooms.mdd_api.auth.dto.TokenResponse;
import com.openclassrooms.mdd_api.auth.validation.PasswordPolicy;
import com.openclassrooms.mdd_api.common.web.exception.ApiBadRequestException;
import com.openclassrooms.mdd_api.common.web.exception.ApiConflictException;
import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
import com.openclassrooms.mdd_api.common.web.response.FieldErrorItem;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    public record TokenBundle(TokenResponse tokenResponse, String refreshTokenRaw) {}

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // - email: trim + lowercase
    // - username: trim (case preserved)
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static String normalizeUsername(String username) {
        return username == null ? null : username.trim();
    }

    private static String normalizeIdentifier(String identifier) {
        return identifier == null ? null : identifier.trim();
    }

    @Transactional
    public Long register(RegisterRequest req) {
        if (!PasswordPolicy.isValid(req.password())) {
            throw new ApiBadRequestException(
                    "Password policy not respected",
                    List.of(new FieldErrorItem(
                            "password",
                            "Must be >=8 and contain lower, upper, digit and special character"
                    ))
            );
        }

        String email = normalizeEmail(req.email());
        String username = normalizeUsername(req.username());

        if (userRepository.existsByEmail(email) || userRepository.existsByUsername(username)) {
            throw new ApiConflictException("Email or username already used");
        }

        try {
            User u = new User(email, username, passwordEncoder.encode(req.password()));
            return userRepository.save(u).getId();
        } catch (DataIntegrityViolationException e) {
            // Anti-race (double submit / concurrence) : on mappe en 409 sans fuite SQL
            throw new ApiConflictException("Email or username already used");
        }
    }

    @Transactional
    public TokenBundle login(LoginRequest req) {
        String identifier = normalizeIdentifier(req.identifier());
        String emailCandidate = normalizeEmail(identifier);

        User user = userRepository.findByEmail(emailCandidate)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        TokenResponse access = jwtService.issueAccessToken(user);
        var refresh = refreshTokenService.issueSingleSession(user);

        return new TokenBundle(access, refresh.rawToken());
    }

    @Transactional
    public TokenBundle refresh(String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            throw new ApiUnauthorizedException("Missing refresh token");
        }

        var rotated = refreshTokenService.rotate(refreshTokenRaw);

        User user = userRepository.findById(rotated.userId())
                .orElseThrow(() -> new ApiUnauthorizedException("Invalid refresh token"));

        TokenResponse access = jwtService.issueAccessToken(user);
        return new TokenBundle(access, rotated.issued().rawToken());
    }

    /**
     * logout is 🔒 (Bearer required by SecurityConfig) + refresh cookie required + CSRF required.
     */
    @Transactional
    public void logout(String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            throw new ApiUnauthorizedException("Missing refresh token");
        }
        refreshTokenService.revoke(refreshTokenRaw);
    }
}
