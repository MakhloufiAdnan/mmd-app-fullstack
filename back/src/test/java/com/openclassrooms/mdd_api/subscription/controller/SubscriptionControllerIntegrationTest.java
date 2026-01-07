package com.openclassrooms.mdd_api.subscription.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.mdd_api.auth.repository.RefreshTokenRepository;
import com.openclassrooms.mdd_api.comment.repository.CommentRepository;
import com.openclassrooms.mdd_api.post.repository.PostRepository;
import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.support.AbstractMySqlIntegrationTest;
import com.openclassrooms.mdd_api.support.TestTopicSeeder;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link com.openclassrooms.mdd_api.subscription.controller.SubscriptionController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionControllerIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired TopicRepository topicRepository;

    @Autowired CommentRepository commentRepository;
    @Autowired PostRepository postRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired UserRepository userRepository;

    private record RegisterPayload(String email, String username, String password) {}
    private record LoginPayload(String identifier, String password) {}
    private record CsrfBundle(String token, Cookie cookie) {}
    private record SubscribePayload(Long topicId) {}

    @BeforeEach
    void setup() {
        // Arrange: reset tables dépendantes (sans toucher topics)
        commentRepository.deleteAllInBatch();
        postRepository.deleteAllInBatch();
        subscriptionRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // Arrange: topics minimum garantis
        TestTopicSeeder.ensureTopicsExist(topicRepository, "Java");
    }

    @Test
    @DisplayName("POST subscribe -> 403 when CSRF missing (even if no bearer)")
    void subscribe_without_csrf_returns_403() throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(new SubscribePayload(1L));

        // Act + Assert
        mockMvc.perform(post("/api/users/me/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST subscribe -> 401 when bearer missing but CSRF provided")
    void subscribe_without_bearer_but_with_csrf_returns_401() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        String body = objectMapper.writeValueAsString(new SubscribePayload(1L));

        // Act + Assert
        mockMvc.perform(post("/api/users/me/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST subscribe -> 201 happy path")
    void subscribe_happy_path_returns_201_and_id() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "user@example.com", "Alice", "Aa1!aaaa");
        String accessToken = accessToken(login(csrf, "user@example.com", "Aa1!aaaa"));

        long topicId = topicRepository.findAll().getFirst().getId();

        // Act + Assert
        mockMvc.perform(post("/api/users/me/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscribePayload(topicId)))
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value((int) topicId));
    }

    @Test
    @DisplayName("POST subscribe twice -> 409")
    void subscribe_twice_returns_409() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "user@example.com", "Alice", "Aa1!aaaa");
        String accessToken = accessToken(login(csrf, "user@example.com", "Aa1!aaaa"));

        long topicId = topicRepository.findAll().getFirst().getId();

        // 1st subscribe OK
        mockMvc.perform(post("/api/users/me/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscribePayload(topicId)))
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isCreated());

        // 2nd subscribe -> 409
        mockMvc.perform(post("/api/users/me/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscribePayload(topicId)))
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE unsubscribe -> 204 idempotent (even if not subscribed)")
    void unsubscribe_is_idempotent_returns_204_even_if_not_subscribed() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "user@example.com", "Alice", "Aa1!aaaa");
        String accessToken = accessToken(login(csrf, "user@example.com", "Aa1!aaaa"));

        long topicId = topicRepository.findAll().getFirst().getId();

        // Act + Assert (not subscribed yet)
        mockMvc.perform(delete("/api/users/me/subscriptions/{topicId}", topicId)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // Act + Assert (still 204)
        mockMvc.perform(delete("/api/users/me/subscriptions/{topicId}", topicId)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    // ---------------- Helpers ----------------

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

    private String accessToken(MvcResult loginRes) throws Exception {
        return objectMapper.readTree(loginRes.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private static String extractCookieValueFromSetCookieHeaders(List<String> setCookieHeaders, String cookieName) {
        String header = setCookieHeaders.stream()
                .filter(h -> h.startsWith(cookieName + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing Set-Cookie header for " + cookieName));

        int start = header.indexOf(cookieName + "=") + cookieName.length() + 1;
        int end = header.indexOf(';', start);
        return header.substring(start, end);
    }
}
