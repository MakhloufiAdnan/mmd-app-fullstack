package com.openclassrooms.mdd_api.auth.service;

import com.openclassrooms.mdd_api.auth.dto.TokenResponse;
import com.openclassrooms.mdd_api.common.config.OcAppProperties;
import com.openclassrooms.mdd_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String ISSUER = "mdd-api";
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

        // IMPORTANT : header explicite pour forcer HS256 (sinon Nimbus peut échouer à sélectionner la clé)
        JwsHeader jwsHeader = JwsHeader.with(SIGNING_ALG)
                .type("JWT")
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims))
                .getTokenValue();

        return new TokenResponse(token, "Bearer", props.getJwtExpirationMs() / 1000);
    }
}
