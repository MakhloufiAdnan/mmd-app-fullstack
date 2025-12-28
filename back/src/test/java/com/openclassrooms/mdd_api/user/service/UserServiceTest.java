package com.openclassrooms.mdd_api.user.service;

import com.openclassrooms.mdd_api.common.web.exception.ApiBadRequestException;
import com.openclassrooms.mdd_api.common.web.exception.ApiConflictException;
import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
import com.openclassrooms.mdd_api.user.dto.UpdateMeRequest;
import com.openclassrooms.mdd_api.user.dto.UserMeResponse;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link UserService}.
 * SUT: UserService
 * Scope:
 * - getMe(): charge l'utilisateur, mappe vers UserMeResponse (subscriptions vide)
 * - updateMe(): applique email/username/password (validation, normalisation, conflicts, mapping 409)
 * Design:
 * - UserRepository et PasswordEncoder sont mockés
 * - Mockito en mode strict: on ne stubbe que ce qui est effectivement utilisé par le test
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    @Test
    void getMe_returns_profile_with_empty_subscriptions() {
        // Arrange
        User u = mock(User.class);
        when(u.getId()).thenReturn(42L);
        when(u.getEmail()).thenReturn("user@example.com");
        when(u.getUsername()).thenReturn("Alice");

        when(userRepository.findById(42L)).thenReturn(Optional.of(u));

        // Act
        UserMeResponse res = userService.getMe(42L);

        // Assert
        assertThat(res.id()).isEqualTo(42L);
        assertThat(res.email()).isEqualTo("user@example.com");
        assertThat(res.username()).isEqualTo("Alice");
        assertThat(res.subscriptions()).isNotNull().isEmpty();
    }

    @Test
    void getMe_unknown_user_throws_unauthorized() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> userService.getMe(99L))
                .isInstanceOf(ApiUnauthorizedException.class)
                .hasMessage("Unauthorized");
    }

    @Test
    void updateMe_unknown_user_throws_unauthorized() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, new UpdateMeRequest(null, null, null)))
                .isInstanceOf(ApiUnauthorizedException.class)
                .hasMessage("Unauthorized");
    }

    @Test
    void updateMe_returns_false_when_request_empty() {
        // Arrange
        User u = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        // Act
        boolean updated = userService.updateMe(1L, new UpdateMeRequest(null, null, null));

        // Assert
        assertThat(updated).isFalse();
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
        verify(userRepository, never()).existsByUsernameAndIdNot(anyString(), anyLong());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateMe_updates_email_normalizes_and_saves() {
        // Arrange
        User u = mock(User.class);
        when(u.getId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(false);

        // Act
        boolean updated = userService.updateMe(1L, new UpdateMeRequest("  NEW@Example.Com  ", null, null));

        // Assert
        assertThat(updated).isTrue();
        verify(u).setEmail("new@example.com");
        verify(userRepository).save(u);
    }

    @Test
    void updateMe_returns_false_when_email_same() {
        // Arrange
        User u = mock(User.class);
        when(u.getEmail()).thenReturn("user@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        // Act
        boolean updated = userService.updateMe(1L, new UpdateMeRequest(" User@Example.Com ", null, null));

        // Assert
        assertThat(updated).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMe_rejects_blank_email() {
        // Arrange
        User u = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, new UpdateMeRequest("   ", null, null)))
                .isInstanceOf(ApiBadRequestException.class)
                .hasMessage("Validation error");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMe_rejects_email_conflict() {
        // Arrange
        User u = mock(User.class);
        when(u.getId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.existsByEmailAndIdNot("taken@example.com", 1L)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, new UpdateMeRequest("taken@example.com", null, null)))
                .isInstanceOf(ApiConflictException.class)
                .hasMessage("Email or username already used");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMe_updates_username_trims_and_saves() {
        // Arrange
        User u = mock(User.class);
        when(u.getId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.existsByUsernameAndIdNot("Alice", 1L)).thenReturn(false);

        // Act
        boolean updated = userService.updateMe(1L, new UpdateMeRequest(null, "  Alice  ", null));

        // Assert
        assertThat(updated).isTrue();
        verify(u).setUsername("Alice");
        verify(userRepository).save(u);
    }

    @Test
    void updateMe_rejects_blank_username() {
        // Arrange
        User u = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, new UpdateMeRequest(null, "   ", null)))
                .isInstanceOf(ApiBadRequestException.class)
                .hasMessage("Validation error");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMe_rejects_username_conflict() {
        // Arrange
        User u = mock(User.class);
        when(u.getId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.existsByUsernameAndIdNot("Bob", 1L)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, new UpdateMeRequest(null, "Bob", null)))
                .isInstanceOf(ApiConflictException.class)
                .hasMessage("Email or username already used");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMe_rejects_invalid_password_policy() {
        // Arrange
        User u = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, new UpdateMeRequest(null, null, "short")))
                .isInstanceOf(ApiBadRequestException.class)
                .hasMessage("Password policy not respected");

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMe_updates_password_encodes_and_saves() {
        // Arrange
        User u = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("Aa1!aaaa")).thenReturn("ENC");

        // Act
        boolean updated = userService.updateMe(1L, new UpdateMeRequest(null, null, "Aa1!aaaa"));

        // Assert
        assertThat(updated).isTrue();
        verify(u).setPasswordHash("ENC");
        verify(userRepository).save(u);
    }

    @Test
    void updateMe_maps_data_integrity_violation_to_409() {
        // Arrange
        User u = mock(User.class);
        when(u.getId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("boom")).when(userRepository).save(u);

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, new UpdateMeRequest("new@example.com", null, null)))
                .isInstanceOf(ApiConflictException.class)
                .hasMessage("Email or username already used");
    }
}
