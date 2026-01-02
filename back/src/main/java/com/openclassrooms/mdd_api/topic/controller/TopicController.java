package com.openclassrooms.mdd_api.topic.controller;

import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
@Tag(name = "Topics", description = "List topics and subscription status")
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    @Operation(summary = "List all topics with subscribed status")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Topics list")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public List<TopicListItemDto> listTopics(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = parseUserId(jwt);
        return topicService.listTopics(userId);
    }

    private Long parseUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ApiUnauthorizedException("Unauthorized");
        }
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new ApiUnauthorizedException("Unauthorized");
        }
    }
}
