package com.openclassrooms.mdd_api.common.security;

import com.openclassrooms.mdd_api.common.config.OcAppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtConfig}.
 *
 * SUT:
 *  - JwtConfig: secret -> key bytes (HEX-even vs UTF-8), HS256 key length guard, encoder/decoder.
 */
class JwtConfigTest {

    @Test
    @DisplayName("toKeyBytes: HEX secret (even length) parsed as bytes (covers trim)")
    void toKeyBytes_hexEven_parsed() throws Exception {
        // Arrange
        byte[] raw = "0123456789ABCDEF0123456789ABCDEF".getBytes(StandardCharsets.UTF_8); // 32 bytes
        String hex = HexFormat.of().formatHex(raw);

        // Act
        byte[] bytes = invokeToKeyBytes("  " + hex + "  ");

        // Assert
        assertThat(bytes).isEqualTo(raw);
    }

    @Test
    @DisplayName("toKeyBytes: HEX odd length falls back to UTF-8 bytes")
    void toKeyBytes_hexOdd_fallsBackUtf8() throws Exception {
        // Arrange
        String oddHex = "abc";

        // Act
        byte[] bytes = invokeToKeyBytes(oddHex);

        // Assert
        assertThat(bytes).isEqualTo(oddHex.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("jwtSecretKey: throws when derived key is < 32 bytes")
    void jwtSecretKey_throws_whenTooShort() {
        // Arrange
        OcAppProperties props = new OcAppProperties();
        props.setJwtSecret("short");
        JwtConfig cfg = new JwtConfig(props);

        // Act + Assert
        assertThatThrownBy(cfg::jwtSecretKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short");
    }

    @Test
    @DisplayName("jwtSecretKey: null secret -> empty -> throws (covers null branch)")
    void jwtSecretKey_nullSecret_throws() {
        // Arrange
        OcAppProperties props = new OcAppProperties();
        props.setJwtSecret(null);
        JwtConfig cfg = new JwtConfig(props);

        // Act + Assert
        assertThatThrownBy(cfg::jwtSecretKey)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too short");
    }

    @Test
    @DisplayName("encoder/decoder: round-trip HS256 JWT with UTF-8 secret (>=32 bytes)")
    void jwt_roundTrip_hs256() {
        // Arrange
        OcAppProperties props = new OcAppProperties();
        props.setJwtSecret("0123456789ABCDEF0123456789ABCDEF!"); // UTF-8 branch, >= 32 bytes
        JwtConfig cfg = new JwtConfig(props);

        SecretKey key = cfg.jwtSecretKey();
        JwtEncoder encoder = cfg.jwtEncoder(key);
        JwtDecoder decoder = cfg.jwtDecoder(key);

        Instant iat = Instant.parse("2030-01-01T00:00:00Z");
        Instant exp = Instant.parse("2030-01-01T01:00:00Z");

        // IMPORTANT: 'iss' must be a valid URL string because Spring converts it to java.net.URL
        String issuerUrl = "https://test";

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuerUrl)
                .subject("123")
                .issuedAt(iat)
                .expiresAt(exp)
                .claim("username", "Alice")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();

        // Act
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        Jwt decoded = decoder.decode(token);

        // Assert
        assertThat(decoded)
                .extracting(Jwt::getSubject, j -> j.getClaim("username"))
                .containsExactly("123", "Alice");

        assertThat(decoded.getIssuer()).hasToString(issuerUrl);
    }

    @SuppressWarnings("java:S3011") // reflective access is intentional to cover private branch logic
    private static byte[] invokeToKeyBytes(String secret) throws Exception {
        Method m = JwtConfig.class.getDeclaredMethod("toKeyBytes", String.class);
        m.setAccessible(true);
        return (byte[]) m.invoke(null, secret);
    }
}
