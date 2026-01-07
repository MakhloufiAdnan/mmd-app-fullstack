package com.openclassrooms.mdd_api.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PostController.
 * Auth strategy:
 *  - We bypass login/refresh here using spring-security-test jwt()
 *  - BUT we still seed a real User in DB because services query userRepository by ID (jwt subject).
 *
 * CSRF strategy:
 *  - We call /api/auth/csrf to obtain XSRF-TOKEN cookie, then send X-XSRF-TOKEN header on write requests.
 */
@org.springframework.boot.test.context.SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class PostControllerIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired TopicRepository topicRepository;
    @Autowired UserRepository userRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired com.openclassrooms.mdd_api.post.repository.PostRepository postRepository;

    // Small local DTO for request body (keeps tests decoupled from package names)
    private record CreatePostPayload(Long topicId, String title, String content) {}
    private record CsrfBundle(String token, Cookie cookie) {}

    @Test
    @DisplayName("POST /api/posts -> 201 when subscribed (happy path)")
    void createPost_whenSubscribed_returns201() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();

        User user = seedUser("user1@example.com", "user1");
        Topic topic = seedTopic("Java");
        seedSubscription(user, topic);

        String body = objectMapper.writeValueAsString(
                new CreatePostPayload(topic.getId(), "Hello", "World")
        );

        // Act + Assert
        mockMvc.perform(post("/api/posts")
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
    @DisplayName("POST /api/posts -> 403 when NOT subscribed (business rule)")
    void createPost_whenNotSubscribed_returns403() throws Exception {
        // Arrange
        CsrfBundle csrf = initCsrf();

        User user = seedUser("user2@example.com", "user2");
        Topic topic = seedTopic("Spring");

        String body = objectMapper.writeValueAsString(
                new CreatePostPayload(topic.getId(), "Title", "Content")
        );

        // Act + Assert
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .cookie(csrf.cookie())
                        .header(CSRF_HEADER, csrf.token())
                        .with(jwtUser(user.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/posts/{id} -> 200 (read is allowed for authenticated users)")
    void getPostDetail_returns200_and_shape() throws Exception {
        // Arrange
        User user = seedUser("user3@example.com", "user3");
        Topic topic = seedTopic("Angular");

        Post post = postRepository.save(new Post("T", "C", topic, user));

        // Act + Assert
        mockMvc.perform(get("/api/posts/{postId}", post.getId())
                        .with(jwtUser(user.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                // Shape checks (contract stability)
                .andExpect(jsonPath("$.id").value(post.getId()))
                .andExpect(jsonPath("$.title").value("T"))
                .andExpect(jsonPath("$.content").value("C"))
                .andExpect(jsonPath("$.createdAt").isString())

                .andExpect(jsonPath("$.topic.id").isNumber())
                .andExpect(jsonPath("$.topic.name").isString())

                .andExpect(jsonPath("$.author.id").isNumber())
                .andExpect(jsonPath("$.author.username").isString())

                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").isEmpty());
    }

    // ---------------------------
    // Helpers
    // ---------------------------

    private RequestPostProcessor jwtUser(Long userId) {
        // subject = userId (string) to match CurrentUserIdExtractor
        return jwt().jwt(j -> j.subject(String.valueOf(userId)));
    }

    private User seedUser(String email, String username) {
        // passwordHash: irrelevant here because auth is bypassed; must be non-null for DB constraints
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
}
