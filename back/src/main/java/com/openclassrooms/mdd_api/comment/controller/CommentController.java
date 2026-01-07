package com.openclassrooms.mdd_api.comment.controller;

import com.openclassrooms.mdd_api.comment.dto.CreateCommentRequest;
import com.openclassrooms.mdd_api.comment.service.CommentService;
import com.openclassrooms.mdd_api.common.security.CurrentUserIdExtractor;
import com.openclassrooms.mdd_api.common.web.response.IdResponse;
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
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Add comments to posts")
public class CommentController {

    private final CommentService commentService;
    private final CurrentUserIdExtractor currentUserIdExtractor;

    @PostMapping
    @Operation(summary = "Add a comment to a post")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "201", description = "Created (returns comment id)")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "CSRF missing/invalid OR not subscribed to topic")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<IdResponse> add(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "X-XSRF-TOKEN", required = false) String xsrfToken,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest req
    ) {
        Long userId = currentUserIdExtractor.requireUserId(jwt);
        Long commentId = commentService.addComment(userId, postId, req);
        return ResponseEntity.status(CREATED).body(new IdResponse(commentId));
    }
}
