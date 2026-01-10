package com.openclassrooms.mdd_api.comment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.mdd_api.common.web.response.ApiErrorCodes;
import com.openclassrooms.mdd_api.post.entity.Post;
import com.openclassrooms.mdd_api.subscription.entity.Subscription;
import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.support.AbstractMySqlIntegrationTest;
import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CommentController.
 */
@org.springframework.boot.test.context.SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class CommentControllerIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired TopicRepository topicRepository;
    @Autowired UserRepository userRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired com.openclassrooms.mdd_api.post.repository.PostRepository postRepository;

    private record CreateCommentPayload(String content) {}
    private record CsrfBundle(String token, Cookie cookie) {}

    // Auth helpers (pour les tests où on veut un vrai Bearer header)
    private record RegisterPayload(String email, String username, String password) {}
    private record LoginPayload(String identifier, String password) {}

    @Test
    @DisplayName("POST /api/posts/{id}/comments -> 201 when subscribed (happy path)")
    void addComment_whenSubscribed_returns201() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();

        User user = seedUser("c1@example.com", "c1");
        Topic topic = seedTopic("Docker");
        seedSubscription(user, topic);

        Post post = postRepository.save(new Post("T", "C", topic, user));

        String body = objectMapper.writeValueAsString(new CreateCommentPayload("Hello"));

        // Act + Assert
        mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .with(jwtUser(user.getId())))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("POST /api/posts/{id}/comments -> 404 when post not found")
    void addComment_postNotFound_returns404() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();

        User user = seedUser("c2@example.com", "c2");
        String body = objectMapper.writeValueAsString(new CreateCommentPayload("Hello"));

        long missingPostId = 999_999L;

        // Act + Assert
        mockMvc.perform(post("/api/posts/{postId}/comments", missingPostId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .with(jwtUser(user.getId())))
                .andExpect(status().isNotFound());
    }

    // -----------------
    // Tests cas limites
    // -----------------

    @Test
    @DisplayName("POST /api/posts/{id}/comments -> 401 when bearer missing (but CSRF provided)")
    void addComment_withoutBearer_but_withCsrf_returns401() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        String body = objectMapper.writeValueAsString(new CreateCommentPayload("Hello"));

        // Act
        var action = mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .cookie(csrf.cookie())
                .header(CSRF_HEADER, csrf.token()));

        // Assert
        action.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(ApiErrorCodes.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    @DisplayName("POST /api/posts/{id}/comments -> 201 even without CSRF when using Bearer (CSRF applies to cookie-based auth)")
    void addComment_withBearer_but_withoutCsrf_returns201() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();

        // On crée un user + token réel pour être proche du comportement navigateur
        register(csrf, "no-csrf-comment@example.com", "noCsrfComment", "Aa1!aaaa");
        String token = accessToken(login(csrf, "no-csrf-comment@example.com", "Aa1!aaaa"));

        User user = userRepository.findByEmail("no-csrf-comment@example.com")
                .orElseThrow(() -> new AssertionError("User must exist"));

        // On crée un post existant (sinon on tomberait en 404)
        Topic topic = seedTopic("NoCsrfTopic");
        seedSubscription(user, topic);
        Post post = postRepository.save(new Post("T", "C", topic, user));

        String body = objectMapper.writeValueAsString(new CreateCommentPayload("Hello"));

        // Act (IMPORTANT : pas de cookie CSRF, pas de header CSRF)
        var action = mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        // Assert
        action.andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber());
    }


    @Test
    @DisplayName("POST /api/posts/{id}/comments -> 403 when NOT subscribed (business rule)")
    void addComment_whenNotSubscribed_returns403() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();

        User user = seedUser("not-subscribed@example.com", "notSubscribed");
        Topic topic = seedTopic("Python");
        Post post = postRepository.save(new Post("T", "C", topic, user));

        String body = objectMapper.writeValueAsString(new CreateCommentPayload("Hello"));

        // Act
        var action = mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .cookie(csrf.cookie())
                .header(CSRF_HEADER, csrf.token())
                .with(jwtUser(user.getId())));

        // Assert
        action.andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(ApiErrorCodes.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    @DisplayName("POST /api/posts/{id}/comments -> 400 when body is invalid (validation)")
    void addComment_invalidBody_returns400_withValidationErrors() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();
        User user = seedUser("bad-comment@example.com", "badComment");

        String body = objectMapper.writeValueAsString(new CreateCommentPayload("   "));

        // Act
        var action = mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .cookie(csrf.cookie())
                .header(CSRF_HEADER, csrf.token())
                .with(jwtUser(user.getId())));

        // Assert
        action.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(ApiErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.message").value("Validation error"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("content"));
    }

    @Test
    @DisplayName("POST /api/posts/{id}/comments -> 201 when content length == 2000 (boundary OK)")
    void addComment_contentMax2000_returns201() throws Exception {
        // Arrange (user abonné au topic du post + CSRF + contenu limite)
        CsrfBundle csrf = initCsrf();

        User user = seedUser("c-max2000@example.com", "cMax2000");
        Topic topic = seedTopic("BoundaryComments");
        seedSubscription(user, topic);

        Post post = postRepository.save(new Post("T", "C", topic, user));

        String content2000 = "A".repeat(2000);
        String body = objectMapper.writeValueAsString(new CreateCommentPayload(content2000));

        // Act
        var action = mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .cookie(csrf.cookie())
                .header(CSRF_HEADER, csrf.token())
                .with(jwtUser(user.getId())));

        // Assert
        action.andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("POST /api/posts/{id}/comments -> 400 when content length == 2001 (boundary KO)")
    void addComment_contentTooLong2001_returns400() throws Exception {
        // Arrange (tout OK sauf content > 2000)
        CsrfBundle csrf = initCsrf();

        User user = seedUser("c-max2001@example.com", "cMax2001");
        Topic topic = seedTopic("BoundaryComments2");
        seedSubscription(user, topic);

        Post post = postRepository.save(new Post("T", "C", topic, user));

        String content2001 = "A".repeat(2001); // dépasse @Size(max=2000)
        String body = objectMapper.writeValueAsString(new CreateCommentPayload(content2001));

        // Act
        var action = mockMvc.perform(post("/api/posts/{postId}/comments", post.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .cookie(csrf.cookie())
                .header(CSRF_HEADER, csrf.token())
                .with(jwtUser(user.getId())));

        // Assert (validation contractuelle)
        action.andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value(ApiErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.message").value("Validation error"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("content"))
                .andExpect(jsonPath("$.fieldErrors[0].message").isString());
    }

    // -------
    // Helpers
    // -------

    private RequestPostProcessor jwtUser(Long userId) {
        return jwt().jwt(j -> j.subject(String.valueOf(userId)));
    }

    private User seedUser(String email, String username) {
        return userRepository.save(new User(email, username, "hash"));
    }

    private Topic seedTopic(String name) {
        String desc = "Description " + name + "\nLigne 2\nLigne 3";
        return topicRepository.save(new Topic(name, desc));
    }

    private void seedSubscription(User user, Topic topic) {
        subscriptionRepository.save(new Subscription(user, topic));
    }

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

    private static String extractCookieValueFromSetCookieHeaders(List<String> setCookieHeaders, String cookieName) {
        String header = setCookieHeaders.stream()
                .filter(h -> h.startsWith(cookieName + "="))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing Set-Cookie header for " + cookieName));

        int start = header.indexOf(cookieName + "=") + cookieName.length() + 1;
        int end = header.indexOf(';', start);
        return header.substring(start, end);
    }

    private void register(CsrfBundle csrf, String email, String username, String password) throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(new RegisterPayload(email, username, password));

        // Act + Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isCreated());
    }

    private MvcResult login(CsrfBundle csrf, String identifier, String password) throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(new LoginPayload(identifier, password));

        // Act + Assert
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token()))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String accessToken(MvcResult loginRes) throws Exception {
        JsonNode json = objectMapper.readTree(loginRes.getResponse().getContentAsString());
        JsonNode token = json.get("accessToken");
        assertThat(token).as("accessToken must be present").isNotNull();
        return token.asText();
    }
}
