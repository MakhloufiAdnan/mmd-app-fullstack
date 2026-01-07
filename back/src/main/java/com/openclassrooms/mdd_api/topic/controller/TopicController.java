package com.openclassrooms.mdd_api.topic.controller;

import com.openclassrooms.mdd_api.common.security.CurrentUserIdExtractor;
import com.openclassrooms.mdd_api.topic.dto.TopicListItemDto;
import com.openclassrooms.mdd_api.topic.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
@Tag(name = "Topics", description = "List topics and subscription status")
public class TopicController {

    private final TopicService topicService;
    private final CurrentUserIdExtractor currentUserIdExtractor;

    @GetMapping
    @Operation(summary = "List all topics with subscribed status")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Topics list")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public List<TopicListItemDto> listTopics(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = currentUserIdExtractor.requireUserId(jwt);
        return topicService.listTopics(userId);
    }
}
