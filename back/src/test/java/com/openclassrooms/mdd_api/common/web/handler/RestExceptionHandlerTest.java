package com.openclassrooms.mdd_api.common.web.handler;

import com.openclassrooms.mdd_api.common.web.exception.ApiBadRequestException;
import com.openclassrooms.mdd_api.common.web.exception.ApiConflictException;
import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
import com.openclassrooms.mdd_api.common.web.response.ApiErrorCodes;
import com.openclassrooms.mdd_api.common.web.response.ApiErrorResponse;
import com.openclassrooms.mdd_api.common.web.response.FieldErrorItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * Unit tests for {@link RestExceptionHandler}.
 * Strategy:
 * - Unit: business exceptions + stable technical mappings (no parsing/@Valid pipeline here)
 * - Integration: malformed JSON / Bean Validation are tested via MockMvc integration tests.
 */
class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    @DisplayName("ApiBadRequest -> 400 VALIDATION_ERROR with message + fieldErrors")
    void handleApiBadRequest_mapsTo400() {
        // Arrange
        ApiBadRequestException ex = new ApiBadRequestException(
                "Password policy not respected",
                List.of(new FieldErrorItem("password", "Must be strong"))
        );

        // Act
        var res = handler.handleApiBadRequest(ex);

        // Assert
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiErrorResponse body = res.getBody();
        assertThat(body).isNotNull();
        assertThat(body.error()).isEqualTo(ApiErrorCodes.VALIDATION_ERROR);
        assertThat(body.message()).isEqualTo("Password policy not respected");
        assertThat(body.fieldErrors()).containsExactly(new FieldErrorItem("password", "Must be strong"));
    }

    @Test
    @DisplayName("ApiBadRequest: null fieldErrors -> empty list (covers null-safety branch)")
    void handleApiBadRequest_nullFieldErrors_becomesEmptyList() {
        // Arrange
        ApiBadRequestException ex = spy(new ApiBadRequestException("msg"));
        doReturn(null).when(ex).getFieldErrors();

        // Act
        var res = handler.handleApiBadRequest(ex);

        // Assert
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().fieldErrors()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("BadCredentials -> 401 UNAUTHORIZED with fixed message (no leak)")
    void handleBadCredentials_mapsTo401_fixedMessage() {
        // Arrange
        BadCredentialsException ex = new BadCredentialsException("should-not-leak");

        // Act
        var res = handler.handleBadCredentials(ex);

        // Assert
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().error()).isEqualTo(ApiErrorCodes.UNAUTHORIZED);
        assertThat(res.getBody().message()).isEqualTo("Invalid credentials");
        assertThat(res.getBody().fieldErrors()).isEmpty();
    }

    @Test
    @DisplayName("ApiUnauthorized -> 401 UNAUTHORIZED with message + fieldErrors")
    void handleApiUnauthorized_mapsTo401() {
        // Arrange
        ApiUnauthorizedException ex = new ApiUnauthorizedException(
                "Missing refresh token",
                List.of(new FieldErrorItem("refreshToken", "required"))
        );

        // Act
        var res = handler.handleApiUnauthorized(ex);

        // Assert
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().error()).isEqualTo(ApiErrorCodes.UNAUTHORIZED);
        assertThat(res.getBody().message()).isEqualTo("Missing refresh token");
        assertThat(res.getBody().fieldErrors()).containsExactly(new FieldErrorItem("refreshToken", "required"));
    }

    @Test
    @DisplayName("ApiConflict -> 409 CONFLICT with message + fieldErrors")
    void handleApiConflict_mapsTo409() {
        // Arrange
        ApiConflictException ex = new ApiConflictException(
                "Email already used",
                List.of(new FieldErrorItem("email", "already used"))
        );

        // Act
        var res = handler.handleApiConflict(ex);

        // Assert
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().error()).isEqualTo(ApiErrorCodes.CONFLICT);
        assertThat(res.getBody().message()).isEqualTo("Email already used");
        assertThat(res.getBody().fieldErrors()).containsExactly(new FieldErrorItem("email", "already used"));
    }

    @Test
    @DisplayName("DataIntegrityViolation -> 409 CONFLICT with generic message (no SQL leak)")
    void handleDbConflict_mapsTo409_noLeak() {
        // Arrange
        DataIntegrityViolationException ex = new DataIntegrityViolationException("SQL details...");

        // Act
        var res = handler.handleDbConflict(ex);

        // Assert
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().error()).isEqualTo(ApiErrorCodes.CONFLICT);
        assertThat(res.getBody().message())
                .isEqualTo("Conflict")
                .doesNotContain("SQL");
        assertThat(res.getBody().fieldErrors()).isEmpty();
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400 VALIDATION_ERROR with exception message")
    void handleBadRequest_mapsTo400() {
        // Arrange
        IllegalArgumentException ex = new IllegalArgumentException("Bad arg");

        // Act
        var res = handler.handleBadRequest(ex);

        // Assert
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().error()).isEqualTo(ApiErrorCodes.VALIDATION_ERROR);
        assertThat(res.getBody().message()).isEqualTo("Bad arg");
        assertThat(res.getBody().fieldErrors()).isEmpty();
    }

    @Test
    @DisplayName("IllegalStateException -> 409 CONFLICT with exception message")
    void handleConflict_mapsTo409() {
        // Arrange
        IllegalStateException ex = new IllegalStateException("Conflict msg");

        // Act
        var res = handler.handleConflict(ex);

        // Assert
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().error()).isEqualTo(ApiErrorCodes.CONFLICT);
        assertThat(res.getBody().message()).isEqualTo("Conflict msg");
        assertThat(res.getBody().fieldErrors()).isEmpty();
    }

    @Test
    @DisplayName("Generic Exception -> 500 INTERNAL with generic message")
    void handleGeneric_mapsTo500() {
        // Arrange
        Exception ex = new Exception("boom");

        // Act
        var res = handler.handleGeneric(ex);

        // Assert
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().error()).isEqualTo(ApiErrorCodes.INTERNAL);
        assertThat(res.getBody().message()).isEqualTo("Internal error");
        assertThat(res.getBody().fieldErrors()).isEmpty();
    }
}
