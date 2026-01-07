package com.openclassrooms.mdd_api.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.mdd_api.common.web.response.ApiErrorCodes;
import com.openclassrooms.mdd_api.support.AbstractMySqlIntegrationTest;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration du flux Auth :
 * CSRF -> register -> login (access + refresh cookie) -> refresh (rotation cookie) -> logout (suppression cookie).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String REFRESH_COOKIE = "refreshToken";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired UserRepository userRepository;

    private record RegisterPayload(String email, String username, String password) {}
    private record LoginPayload(String identifier, String password) {}
    private record CsrfBundle(String token, Cookie cookie) {}

    @Test
    void happy_path_csrf_register_login_refresh_logout() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        String registerBody = objectMapper.writeValueAsString(
                new RegisterPayload("User@Example.Com", "  Alice  ", "Aa1!aaaa")
        );

        // Act + Assert (register)
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber());

        // Assert (normalisation côté back)
        assertThat(userRepository.findByEmail("user@example.com")).isPresent();
        assertThat(userRepository.findByUsername("Alice")).isPresent();

        // Arrange (login)
        String loginBody = objectMapper.writeValueAsString(new LoginPayload(" user@example.com ", "Aa1!aaaa"));

        // Act (login)
        MvcResult loginRes = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").isNumber())
                .andReturn();

        // Assert (tokens)
        String accessToken = readJson(loginRes).get("accessToken").asText();
        String refreshToken = extractCookieValueFromSetCookieHeaders(
                loginRes.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
                REFRESH_COOKIE
        );

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        Cookie refreshCookie = new Cookie(REFRESH_COOKIE, refreshToken);
        refreshCookie.setPath("/api/auth");

        // Act (refresh)
        MvcResult refreshRes = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(csrf.cookie(), refreshCookie)
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").isNumber())
                .andReturn();

        // Assert (rotation du refresh cookie)
        String accessToken2 = readJson(refreshRes).get("accessToken").asText();
        String refreshToken2 = extractCookieValueFromSetCookieHeaders(
                refreshRes.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
                REFRESH_COOKIE
        );

        assertThat(accessToken2).isNotBlank();
        assertThat(refreshToken2).isNotBlank().isNotEqualTo(refreshToken);

        Cookie refreshCookie2 = new Cookie(REFRESH_COOKIE, refreshToken2);
        refreshCookie2.setPath("/api/auth");

        // Act (logout)
        MvcResult logoutRes = mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken2)
                        .cookie(csrf.cookie(), refreshCookie2)
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isNoContent())
                .andReturn();

        // Assert (cookie supprimé)
        String deleteSetCookie = findSetCookieHeader(
                logoutRes.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
                REFRESH_COOKIE
        );

        assertThat(deleteSetCookie)
                .contains(REFRESH_COOKIE + "=")
                .contains("Max-Age=0")
                .contains("Path=/api/auth");
    }

    @Test
    void login_invalid_credentials_returns_401_with_contract_payload() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "bob@example.com", "bob", "Aa1!aaaa");

        String loginBody = objectMapper.writeValueAsString(new LoginPayload("bob@example.com", "wrong"));

        // Act
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").isString())
                .andReturn();

        assertThat(readJson(res).get("message").asText()).isNotBlank();
    }

    @Test
    void register_conflict_returns_409_with_contract_payload() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "conflict@example.com", "conflict", "Aa1!aaaa");

        String body = objectMapper.writeValueAsString(
                new RegisterPayload("conflict@example.com", "conflict", "Aa1!aaaa")
        );

        // Act
        MvcResult res = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").isString())
                .andReturn();

        assertThat(readJson(res).get("message").asText()).isNotBlank();
    }

    @Test
    void refresh_missing_csrf_returns_403() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "carol@example.com", "carol", "Aa1!aaaa");

        MvcResult loginRes = login(csrf, "carol@example.com", "Aa1!aaaa");
        String refreshToken = extractCookieValueFromSetCookieHeaders(
                loginRes.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
                REFRESH_COOKIE
        );

        Cookie refreshCookie = new Cookie(REFRESH_COOKIE, refreshToken);
        refreshCookie.setPath("/api/auth");

        // Act + Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(csrf.cookie(), refreshCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_malformed_json_returns_400_with_contract_payload() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        String malformedJson = """
            {"identifier":"bob@example.com","password":
            """;

        // Act + Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(ApiErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void refresh_without_cookie_returns_401_with_contract_payload() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();

        // Act + Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(ApiErrorCodes.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("Missing refresh token"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void refresh_blank_cookie_returns_401_with_contract_payload() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();

        Cookie blankRefresh = new Cookie(REFRESH_COOKIE, "   ");
        blankRefresh.setPath("/api/auth");

        // Act + Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(csrf.cookie(), blankRefresh)
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(ApiErrorCodes.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("Missing refresh token"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    // Helpers

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
        assertThat(value).isNotBlank();

        return value;
    }
}
