package com.openclassrooms.mdd_api.common.security;

import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentUserIdExtractorTest {

    private final CurrentUserIdExtractor extractor = new CurrentUserIdExtractor();

    @Test
    void requireUserId_shouldReturnLong_whenSubjectIsNumeric() {
        Jwt jwt = buildJwtWithSubject("5");
        assertEquals(5L, extractor.requireUserId(jwt));
    }

    @Test
    void requireUserId_shouldThrowUnauthorized_whenJwtIsNull() {
        assertThrows(ApiUnauthorizedException.class, () -> extractor.requireUserId(null));
    }

    @Test
    void requireUserId_shouldThrowUnauthorized_whenSubjectIsNull() {
        Jwt jwt = Jwt.withTokenValue("token")
                .headers(h -> h.putAll(Map.of("alg", "none")))
                .claims(c -> c.put("some", "claim"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertThrows(ApiUnauthorizedException.class, () -> extractor.requireUserId(jwt));
    }

    @Test
    void requireUserId_shouldThrowUnauthorized_whenSubjectIsNotNumeric() {
        Jwt jwt = buildJwtWithSubject("abc");
        assertThrows(ApiUnauthorizedException.class, () -> extractor.requireUserId(jwt));
    }

    private Jwt buildJwtWithSubject(String subject) {
        return Jwt.withTokenValue("token")
                .headers(h -> h.putAll(Map.of("alg", "none")))
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
