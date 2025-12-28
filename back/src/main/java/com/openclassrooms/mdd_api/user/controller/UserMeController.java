package com.openclassrooms.mdd_api.user.controller;

import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
import com.openclassrooms.mdd_api.user.dto.UpdateMeRequest;
import com.openclassrooms.mdd_api.user.dto.UpdatedResponse;
import com.openclassrooms.mdd_api.user.dto.UserMeResponse;
import com.openclassrooms.mdd_api.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints profil utilisateur.
 */
@Tag(name = "Users", description = "User profile endpoints")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserMeController {

    private final UserService userService;

    @Operation(summary = "Get current user profile (me)")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Profile")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        Long userId = parseUserId(jwt);
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @Operation(summary = "Update current user profile (me)")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "403", description = "CSRF missing/invalid")
    @ApiResponse(responseCode = "409", description = "Conflict (email/username already used)")
    @PutMapping("/me")
    public ResponseEntity<UpdatedResponse> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateMeRequest request
    ) {
        Long userId = parseUserId(jwt);
        boolean updated = userService.updateMe(userId, request);
        return ResponseEntity.ok(new UpdatedResponse(updated));
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
