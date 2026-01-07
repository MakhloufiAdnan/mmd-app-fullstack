package com.openclassrooms.mdd_api.subscription.controller;

import com.openclassrooms.mdd_api.common.security.CurrentUserIdExtractor;
import com.openclassrooms.mdd_api.common.web.response.IdResponse;
import com.openclassrooms.mdd_api.subscription.dto.SubscribeRequest;
import com.openclassrooms.mdd_api.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/users/me/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Subscribe / unsubscribe to topics")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUserIdExtractor currentUserIdExtractor;

    @PostMapping
    @Operation(summary = "Subscribe to a topic")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "201", description = "Subscribed (returns topic id)")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "CSRF missing/invalid")
    @ApiResponse(responseCode = "404", description = "Topic not found")
    @ApiResponse(responseCode = "409", description = "Already subscribed")
    public ResponseEntity<IdResponse> subscribe(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "X-XSRF-TOKEN", required = false) String xsrfToken,
            @Valid @RequestBody SubscribeRequest req
    ) {
        Long userId = currentUserIdExtractor.requireUserId(jwt);
        Long topicId = subscriptionService.subscribe(userId, req.topicId());
        return ResponseEntity.status(CREATED).body(new IdResponse(topicId));
    }

    @DeleteMapping("/{topicId}")
    @Operation(summary = "Unsubscribe from a topic")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "Unsubscribed")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "CSRF missing/invalid")
    public ResponseEntity<Void> unsubscribe(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "X-XSRF-TOKEN", required = false) String xsrfToken,
            @PathVariable Long topicId
    ) {
        Long userId = currentUserIdExtractor.requireUserId(jwt);
        subscriptionService.unsubscribe(userId, topicId);
        return ResponseEntity.noContent().build();
    }
}
