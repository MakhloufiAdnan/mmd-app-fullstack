package com.openclassrooms.mdd_api.post.controller;

import com.openclassrooms.mdd_api.common.security.CurrentUserIdExtractor;
import com.openclassrooms.mdd_api.common.web.response.IdResponse;
import com.openclassrooms.mdd_api.post.dto.CreatePostRequest;
import com.openclassrooms.mdd_api.post.dto.PostDetailResponse;
import com.openclassrooms.mdd_api.post.service.PostService;
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
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Posts", description = "Create and read posts")
public class PostController {

    private final PostService postService;
    private final CurrentUserIdExtractor currentUserIdExtractor;

    @PostMapping
    @Operation(summary = "Create a post")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "201", description = "Created (returns post id)")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "CSRF missing/invalid OR not subscribed to topic")
    @ApiResponse(responseCode = "404", description = "Topic not found")
    public ResponseEntity<IdResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "X-XSRF-TOKEN", required = false) String xsrfToken,
            @Valid @RequestBody CreatePostRequest req
    ) {
        Long userId = currentUserIdExtractor.requireUserId(jwt);
        Long postId = postService.createPost(userId, req);
        return ResponseEntity.status(CREATED).body(new IdResponse(postId));
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get post details")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Post details")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Post not found")
    public ResponseEntity<PostDetailResponse> getById(@PathVariable Long postId) {
        PostDetailResponse detail = postService.getPostDetail(postId);
        return ResponseEntity.ok(detail);
    }
}
