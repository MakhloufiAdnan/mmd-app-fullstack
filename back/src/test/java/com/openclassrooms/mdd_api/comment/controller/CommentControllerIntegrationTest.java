package com.openclassrooms.mdd_api.comment.controller;

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

    // ---------------------------
    // Helpers
    // ---------------------------

    private RequestPostProcessor jwtUser(Long userId) {
        return jwt().jwt(j -> j.subject(String.valueOf(userId)));
    }

    private User seedUser(String email, String username) {
        return userRepository.save(new User(email, username, "hash"));
    }

    private Topic seedTopic(String name) {
        return topicRepository.save(new Topic(name));
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
