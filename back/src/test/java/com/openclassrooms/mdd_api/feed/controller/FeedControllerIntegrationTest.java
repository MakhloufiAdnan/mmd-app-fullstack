package com.openclassrooms.mdd_api.feed.controller;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link FeedController}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FeedControllerIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired TopicRepository topicRepository;

    // DB cleanup (stabilité / pas de conflits entre tests)
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
    @DisplayName("GET /api/feed -> 401 when no bearer")
    void get_feed_without_bearer_returns_401() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/api/feed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/feed -> 200 empty when user has no subscriptions")
    void get_feed_when_user_has_no_subscriptions_returns_empty_list() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "user@example.com", "Alice", "Aa1!aaaa");
        String accessToken = accessToken(login(csrf, "user@example.com", "Aa1!aaaa"));

        // Act + Assert
        mockMvc.perform(get("/api/feed")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/feed?topicId=X -> 200 empty when not subscribed to that topic")
    void get_feed_with_topicId_not_subscribed_returns_empty_list() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "user@example.com", "Alice", "Aa1!aaaa");
        String accessToken = accessToken(login(csrf, "user@example.com", "Aa1!aaaa"));

        long notSubscribedTopicId = 999_999L;

        // Act + Assert
        mockMvc.perform(get("/api/feed")
                        .param("topicId", String.valueOf(notSubscribedTopicId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/feed -> 200 array after subscription (may be empty if no posts exist)")
    void get_feed_after_subscribe_returns_200_and_array() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        register(csrf, "user@example.com", "Alice", "Aa1!aaaa");
        String accessToken = accessToken(login(csrf, "user@example.com", "Aa1!aaaa"));

        long topicId = topicRepository.findAll().getFirst().getId();

        // Subscribe
        mockMvc.perform(post("/api/users/me/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubscribePayload(topicId)))
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value((int) topicId));

        // Act
        MvcResult res = mockMvc.perform(get("/api/feed")
                        .param("topicId", String.valueOf(topicId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        // Assert (shape only; content may be empty if no posts exist)
        JsonNode body = readJson(res);
        assertThat(body.isArray()).isTrue();
    }

    // ---------------- Helpers  ----------------

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
        return readJson(loginRes).get("accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
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
