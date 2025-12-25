package com.openclassrooms.mdd_api.auth.service;

import com.openclassrooms.mdd_api.auth.dto.LoginRequest;
import com.openclassrooms.mdd_api.auth.dto.RegisterRequest;
import com.openclassrooms.mdd_api.auth.dto.TokenResponse;
import com.openclassrooms.mdd_api.auth.validation.PasswordPolicy;
import com.openclassrooms.mdd_api.common.web.ApiBadRequestException;
import com.openclassrooms.mdd_api.common.web.ApiConflictException;
import com.openclassrooms.mdd_api.common.web.FieldErrorItem;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public Long register(RegisterRequest req) {
        if (!PasswordPolicy.isValid(req.password())) {
            // Conforme contrat : 400 VALIDATION_ERROR + fieldErrors possible
            throw new ApiBadRequestException(
                    "Password policy not respected",
                    List.of(new FieldErrorItem("password",
                            "Must be >=8 and contain lower, upper, digit and special character"))
            );
        }

        if (userRepository.existsByEmail(req.email()) || userRepository.existsByUsername(req.username())) {
            // Conforme contrat : 409 CONFLICT
            throw new ApiConflictException("Email or username already used");
        }

        User u = new User(req.email(), req.username(), passwordEncoder.encode(req.password()));
        return userRepository.save(u).getId();
    }

    @Transactional
    public TokenBundle login(LoginRequest req) {
        User user = userRepository.findByEmail(req.identifier())
                .or(() -> userRepository.findByUsername(req.identifier()))
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
        var rotated = refreshTokenService.rotate(refreshTokenRaw);

        // Reload "safe" : User pleinement initialisé
        User user = userRepository.findById(rotated.userId())
                .orElseThrow(() -> new com.openclassrooms.mdd_api.common.web.ApiUnauthorizedException("Invalid refresh token"));

        TokenResponse access = jwtService.issueAccessToken(user);
        return new TokenBundle(access, rotated.issued().rawToken());
    }

    @Transactional
    public void logout(String refreshTokenRaw) {
        refreshTokenService.revoke(refreshTokenRaw);
    }
}
