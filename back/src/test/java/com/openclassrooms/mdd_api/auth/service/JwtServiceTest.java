package com.openclassrooms.mdd_api.auth.service;

import com.openclassrooms.mdd_api.auth.dto.TokenResponse;
import com.openclassrooms.mdd_api.common.config.OcAppProperties;
import com.openclassrooms.mdd_api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock JwtEncoder jwtEncoder;
    @Mock OcAppProperties props;

    @InjectMocks JwtService jwtService;

    @Test
    @DisplayName("issueAccessToken: builds HS256/JWT header + expected claims, and returns TokenResponse")
    void issueAccessToken_buildsClaimsAndReturnsResponse() {
        // Arrange
        long jwtExpirationMs = 60_000L; // 60s (multiple de 1000 => test non ambigu)
        when(props.getJwtExpirationMs()).thenReturn(jwtExpirationMs);

        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(user.getUsername()).thenReturn("JohnDoe");

        // JwtEncoder.encode(...) renvoie un Jwt dont getTokenValue() sera utilisé
        Jwt encoded = new Jwt(
                "ENCODED_TOKEN",
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(60),
                Map.of("alg", "HS256"),
                Map.of("sub", "42")
        );
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(encoded);

        // Act
        TokenResponse response = jwtService.issueAccessToken(user);

        // Assert (retour)
        assertThat(response).isEqualTo(new TokenResponse("ENCODED_TOKEN", "Bearer", 60));

        // Assert (contenu envoyé à l’encodeur)
        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());

        JwtEncoderParameters params = captor.getValue();

        JwsHeader header = params.getJwsHeader();
        assertThat(header).isNotNull();
        assertThat(header.getAlgorithm()).isEqualTo(MacAlgorithm.HS256);
        assertThat(header.getType()).isEqualTo("JWT");

        JwtClaimsSet claimsSet = params.getClaims();
        Map<String, Object> claims = claimsSet.getClaims();

        assertThat(claimsSet.hasClaim("iss")).isTrue();
        assertThat(claimsSet.getClaimAsString("iss")).isEqualTo("mdd-api");

        assertThat(claimsSet.hasClaim("sub")).isTrue();
        assertThat(claimsSet.getClaimAsString("sub")).isEqualTo("42");

        assertThat(claimsSet.hasClaim("username")).isTrue();
        assertThat(claimsSet.getClaimAsString("username")).isEqualTo("JohnDoe");


        assertThat(claims.get("iat")).isInstanceOf(Instant.class);
        assertThat(claims.get("exp")).isInstanceOf(Instant.class);

        Instant iat = (Instant) claims.get("iat");
        Instant exp = (Instant) claims.get("exp");

        assertThat(Duration.between(iat, exp)).isEqualTo(Duration.ofMillis(jwtExpirationMs));
    }
}
