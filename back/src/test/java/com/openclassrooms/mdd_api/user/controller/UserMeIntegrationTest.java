package com.openclassrooms.mdd_api.user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.mdd_api.auth.repository.RefreshTokenRepository;
import com.openclassrooms.mdd_api.common.web.response.ApiErrorCodes;
import com.openclassrooms.mdd_api.support.AbstractMySqlIntegrationTest;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration des endpoints UserMe :
 * GET /api/users/me (Bearer)
 * PUT /api/users/me (Bearer + CSRF)
 * Flux utilisé : CSRF > register > login > appel endpoint protégé,
 * comme dans AuthFlowIntegrationTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserMeIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private record RegisterPayload(String email, String username, String password) {}
    private record LoginPayload(String identifier, String password) {}
    private record UpdatePayload(String email, String username, String password) {}
    private record CsrfBundle(String token, Cookie cookie) {}

    @BeforeEach
    void resetDatabase() {
        refreshTokenRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void me_happy_path_returns_profile() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "User@Example.Com", "  Alice  ", "Aa1!aaaa");

        MvcResult loginRes = login(csrf, " user@example.com ", "Aa1!aaaa");
        String accessToken = readJson(loginRes).get("accessToken").asText();

        // Act + Assert
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.username").value("Alice"))
                .andExpect(jsonPath("$.subscriptions").isArray())
                .andExpect(jsonPath("$.subscriptions").isEmpty());
    }

    @Test
    void me_without_bearer_returns_401_with_contract_payload() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(ApiErrorCodes.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void update_me_happy_path_updates_email_username_and_password() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "user@example.com", "alice", "Aa1!aaaa");

        MvcResult loginRes = login(csrf, "user@example.com", "Aa1!aaaa");
        String accessToken = readJson(loginRes).get("accessToken").asText();

        String body = objectMapper.writeValueAsString(
                new UpdatePayload("NEW@Example.Com", "  NewName  ", "Bb2!bbbb")
        );

        // Act + Assert (update)
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.updated").value(true));

        // Assert (DB updated + normalized)
        User updated = userRepository.findByEmail("new@example.com")
                .orElseThrow(() -> new AssertionError("User must be found with normalized email"));

        assertThat(updated.getUsername()).isEqualTo("NewName");
        assertThat(passwordEncoder.matches("Bb2!bbbb", updated.getPasswordHash()))
                .as("Password hash must match new password")
                .isTrue();
    }

    @Test
    void update_me_empty_body_returns_updated_false() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "user@example.com", "alice", "Aa1!aaaa");

        MvcResult loginRes = login(csrf, "user@example.com", "Aa1!aaaa");
        String accessToken = readJson(loginRes).get("accessToken").asText();

        // Act + Assert
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(false));
    }

    @Test
    void update_me_conflict_returns_409_with_contract_payload() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();

        // user #1
        register(csrf, "u1@example.com", "u1", "Aa1!aaaa");
        MvcResult loginU1 = login(csrf, "u1@example.com", "Aa1!aaaa");
        String tokenU1 = readJson(loginU1).get("accessToken").asText();

        // user #2 owns the target email
        register(csrf, "taken@example.com", "u2", "Aa1!aaaa");

        String body = objectMapper.writeValueAsString(new UpdatePayload("taken@example.com", null, null));

        // Act
        MvcResult res = mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenU1))
                // Assert
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").isString())
                .andReturn();

        // Assert (message non vide — même pattern que tes tests Auth)
        assertThat(readJson(res).get("message").asText()).isNotBlank();
    }

    @Test
    void update_me_with_bearer_does_not_require_csrf() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "user@example.com", "alice", "Aa1!aaaa");

        MvcResult loginRes = login(csrf, "user@example.com", "Aa1!aaaa");
        String accessToken = readJson(loginRes).get("accessToken").asText();

        String body = objectMapper.writeValueAsString(new UpdatePayload("new@example.com", null, null));

        // Act + Assert (Bearer présent, pas de CSRF)
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void update_me_invalid_password_policy_returns_400_with_contract_payload() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "user@example.com", "alice", "Aa1!aaaa");

        MvcResult loginRes = login(csrf, "user@example.com", "Aa1!aaaa");
        String accessToken = readJson(loginRes).get("accessToken").asText();

        String body = objectMapper.writeValueAsString(new UpdatePayload(null, null, "short"));

        // Act + Assert
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(ApiErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    // Helpers (test only)

    /**
     * Appelle le endpoint public CSRF et retourne token + cookie.
     * Contrat : {@code GET /api/auth/csrf} -> 204 + Set-Cookie XSRF-TOKEN.
     */
    private CsrfBundle initCsrf() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andReturn();

        String csrfToken = extractCookieValueFromSetCookieHeaders(
                res.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
                CSRF_COOKIE
        );

        Cookie csrfCookie = new Cookie(CSRF_COOKIE, csrfToken);
        csrfCookie.setPath("/");

        return new CsrfBundle(csrfToken, csrfCookie);
    }

    private void register(CsrfBundle csrf, String email, String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterPayload(email, username, password));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    private MvcResult login(CsrfBundle csrf, String identifier, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload(identifier, password));

        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String findSetCookieHeader(List<String> setCookieHeaders, String cookieName) {
        return setCookieHeaders.stream()
                .filter(h -> h.startsWith(cookieName + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing Set-Cookie header for " + cookieName));
    }

    private static String extractCookieValueFromSetCookieHeaders(List<String> setCookieHeaders, String cookieName) {
        String header = findSetCookieHeader(setCookieHeaders, cookieName);

        int start = header.indexOf(cookieName + "=") + cookieName.length() + 1;
        int end = header.indexOf(';', start);
        if (end < 0) end = header.length();

        String value = header.substring(start, end);
        assertThat(value)
                .as("Cookie %s doit être présent".formatted(cookieName))
                .isNotBlank();

        return value;
    }
}
