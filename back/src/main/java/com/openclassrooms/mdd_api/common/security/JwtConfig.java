package com.openclassrooms.mdd_api.common.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.openclassrooms.mdd_api.common.config.OcAppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    private final OcAppProperties props;

    @Bean
    public SecretKey jwtSecretKey() {
        byte[] keyBytes = toKeyBytes(props.getJwtSecret());

        // HS256 => 32 bytes minimum (256 bits)
        if (keyBytes.length < 32) {
            throw new IllegalStateException("TOKEN_SECRET too short for HS256 (need >= 32 bytes)");
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Supporte 2 formats:
     * - HEX (si TOKEN_SECRET est une chaîne hex paire)
     * - sinon UTF-8 bytes
     */
    private static byte[] toKeyBytes(String secret) {
        String s = secret == null ? "" : secret.trim();
        if (s.matches("^[0-9a-fA-F]+$") && (s.length() % 2 == 0)) {
            return HexFormat.of().parseHex(s);
        }
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
