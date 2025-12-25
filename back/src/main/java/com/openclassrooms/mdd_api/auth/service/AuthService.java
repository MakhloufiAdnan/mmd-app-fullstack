package com.openclassrooms.mdd_api.auth.service;

import com.openclassrooms.mdd_api.auth.dto.LoginRequest;
import com.openclassrooms.mdd_api.auth.dto.RegisterRequest;
import com.openclassrooms.mdd_api.auth.dto.TokenResponse;
import com.openclassrooms.mdd_api.auth.validation.PasswordPolicy;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            throw new IllegalArgumentException("Password policy not respected");
        }

        if (userRepository.existsByEmail(req.email()) || userRepository.existsByUsername(req.username())) {
            throw new IllegalStateException("Email or username already used");
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
        TokenResponse access = jwtService.issueAccessToken(rotated.user());
        return new TokenBundle(access, rotated.issued().rawToken());
    }

    @Transactional
    public void logout(String refreshTokenRaw) {
        refreshTokenService.revoke(refreshTokenRaw);
    }
}
