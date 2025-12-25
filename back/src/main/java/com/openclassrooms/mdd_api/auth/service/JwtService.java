package com.openclassrooms.mdd_api.auth.service;

import com.openclassrooms.mdd_api.auth.dto.TokenResponse;
import com.openclassrooms.mdd_api.common.config.OcAppProperties;
import com.openclassrooms.mdd_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String ISSUER = "mdd-api";
    private static final String TOKEN_TYPE_BEARER = "Bearer"; // conforme contrat
    private static final MacAlgorithm SIGNING_ALG = MacAlgorithm.HS256;

    private final JwtEncoder jwtEncoder;
    private final OcAppProperties props;

    public TokenResponse issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(props.getJwtExpirationMs());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(exp)
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .build();

        // Header explicite : HS256 (évite ambiguïtés de sélection clé)
        JwsHeader jwsHeader = JwsHeader.with(SIGNING_ALG)
                .type("JWT")
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims))
                .getTokenValue();

        long expiresInSeconds = Duration.between(now, exp).getSeconds();

        return new TokenResponse(token, TOKEN_TYPE_BEARER, expiresInSeconds);
    }
}
