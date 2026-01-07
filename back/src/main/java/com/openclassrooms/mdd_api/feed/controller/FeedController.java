package com.openclassrooms.mdd_api.feed.controller;

import com.openclassrooms.mdd_api.common.security.CurrentUserIdExtractor;
import com.openclassrooms.mdd_api.feed.dto.FeedItemDto;
import com.openclassrooms.mdd_api.feed.service.FeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Feed (articles) : basé sur les abonnements de l'utilisateur connecté.
 * Contrat : GET /api/feed?order=desc|asc&topicId=<id> (topicId optionnel), default order=desc.
 */
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "Feed endpoints (articles based on subscriptions)")
public class FeedController {

    private final FeedService feedService;
    private final CurrentUserIdExtractor currentUserIdExtractor;

    @GetMapping
    @Operation(summary = "Get feed (articles) based on current user's subscriptions")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Feed list")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<List<FeedItemDto>> getFeed(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "topicId", required = false) Long topicId
    ) {
        Long userId = currentUserIdExtractor.requireUserId(jwt);
        return ResponseEntity.ok(feedService.getFeed(userId, order, topicId));
    }
}
