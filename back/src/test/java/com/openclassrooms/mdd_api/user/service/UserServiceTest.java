package com.openclassrooms.mdd_api.user.service;

import com.openclassrooms.mdd_api.common.web.exception.ApiBadRequestException;
import com.openclassrooms.mdd_api.common.web.exception.ApiConflictException;
import com.openclassrooms.mdd_api.common.web.exception.ApiUnauthorizedException;
import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.topic.dto.TopicDto;
import com.openclassrooms.mdd_api.user.dto.UpdateMeRequest;
import com.openclassrooms.mdd_api.user.dto.UserMeResponse;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    @Test
    @DisplayName("getMe: 401 si user inexistant")
    void getMe_unauthorized_whenMissingUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> userService.getMe(1L))
                .isInstanceOf(ApiUnauthorizedException.class)
                .hasMessageContaining("Unauthorized");

        verify(userRepository).findById(1L);
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    @DisplayName("getMe: retourne le profil + subscriptions")
    void getMe_returnsProfile() {
        // Arrange
        User u = mock(User.class);
        when(u.getId()).thenReturn(1L);
        when(u.getEmail()).thenReturn("a@b.com");
        when(u.getUsername()).thenReturn("alice");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        List<TopicDto> subs = List.of(new TopicDto(10L, "Java"));
        when(subscriptionRepository.findSubscribedTopicsByUserId(1L)).thenReturn(subs);

        // Act
        UserMeResponse res = userService.getMe(1L);

        // Assert
        assertThat(res).isEqualTo(new UserMeResponse(1L, "a@b.com", "alice", subs));
    }

    @Test
    @DisplayName("updateMe: 401 si user inexistant")
    void updateMe_unauthorized_whenMissingUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        UpdateMeRequest req = new UpdateMeRequest(null, null, null);

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, req))
                .isInstanceOf(ApiUnauthorizedException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    @DisplayName("updateMe: retourne false et ne save pas si aucun changement (tout null)")
    void updateMe_returnsFalse_whenNoChangesAllNull() {
        // Arrange
        User u = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        UpdateMeRequest req = new UpdateMeRequest(null, null, null);

        // Act
        boolean changed = userService.updateMe(1L, req);

        // Assert
        assertThat(changed).isFalse();
        verify(userRepository, never()).save(any());
        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
        verify(userRepository, never()).existsByUsernameAndIdNot(anyString(), anyLong());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("updateMe: 400 si email blank après normalisation")
    void updateMe_rejectsBlankEmail() {
        // Arrange
        User u = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        UpdateMeRequest req = new UpdateMeRequest("   ", null, null);

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, req))
                .isInstanceOf(ApiBadRequestException.class)
                .hasMessageContaining("Validation error");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMe: 400 si username blank après trim")
    void updateMe_rejectsBlankUsername() {
        // Arrange
        User u = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        UpdateMeRequest req = new UpdateMeRequest(null, "   ", null);

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, req))
                .isInstanceOf(ApiBadRequestException.class)
                .hasMessageContaining("Validation error");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMe: 400 si password ne respecte pas la policy (et n'encode pas)")
    void updateMe_rejectsInvalidPasswordPolicy() {
        // Arrange
        User u = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        UpdateMeRequest req = new UpdateMeRequest(null, null, "short");

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, req))
                .isInstanceOf(ApiBadRequestException.class)
                .hasMessageContaining("Password policy not respected");

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMe: retourne false si email inchangé après trim+lower (aucune vérif d'unicité)")
    void updateMe_returnsFalse_whenEmailUnchangedAfterNormalize() {
        // Arrange
        // IMPORTANT: ici, on ne stubbe PAS existsByEmailAndIdNot(...) car il ne doit jamais être appelé.
        User u = mock(User.class);
        when(u.getEmail()).thenReturn("user@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        UpdateMeRequest req = new UpdateMeRequest("  USER@Example.COM  ", null, null);

        // Act
        boolean changed = userService.updateMe(1L, req);

        // Assert
        assertThat(changed).isFalse();
        verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
        verify(u, never()).setEmail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMe: retourne false si username inchangé après trim (aucune vérif d'unicité)")
    void updateMe_returnsFalse_whenUsernameUnchangedAfterTrim() {
        // Arrange
        User u = mock(User.class);
        when(u.getUsername()).thenReturn("Alice");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        UpdateMeRequest req = new UpdateMeRequest(null, "  Alice  ", null);

        // Act
        boolean changed = userService.updateMe(1L, req);

        // Assert
        assertThat(changed).isFalse();
        verify(userRepository, never()).existsByUsernameAndIdNot(anyString(), anyLong());
        verify(u, never()).setUsername(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMe: 409 si email déjà utilisé par un autre user")
    void updateMe_conflict_whenEmailAlreadyUsed() {
        // Arrange
        User u = mock(User.class);
        when(u.getId()).thenReturn(1L);
        when(u.getEmail()).thenReturn("old@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        when(userRepository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(true);

        UpdateMeRequest req = new UpdateMeRequest("  New@Example.Com ", null, null);

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, req))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("Email or username already used");

        verify(u, never()).setEmail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMe: 409 si username déjà utilisé par un autre user")
    void updateMe_conflict_whenUsernameAlreadyUsed() {
        // Arrange
        User u = mock(User.class);
        when(u.getId()).thenReturn(1L);
        when(u.getUsername()).thenReturn("old");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        when(userRepository.existsByUsernameAndIdNot("NewName", 1L)).thenReturn(true);

        UpdateMeRequest req = new UpdateMeRequest(null, "  NewName  ", null);

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, req))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("Email or username already used");

        verify(u, never()).setUsername(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMe: met à jour l'email (normalize) et save")
    void updateMe_updatesEmail_andSaves() {
        // Arrange
        User u = mock(User.class);
        when(u.getId()).thenReturn(1L);
        when(u.getEmail()).thenReturn("old@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        when(userRepository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(false);

        UpdateMeRequest req = new UpdateMeRequest("  New@Example.Com ", null, null);

        // Act
        boolean changed = userService.updateMe(1L, req);

        // Assert
        assertThat(changed).isTrue();
        verify(u).setEmail("new@example.com");
        verify(userRepository).save(u);
    }

    @Test
    @DisplayName("updateMe: met à jour le username (trim) et save")
    void updateMe_updatesUsername_andSaves() {
        // Arrange
        User u = mock(User.class);
        when(u.getId()).thenReturn(1L);
        when(u.getUsername()).thenReturn("old");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        when(userRepository.existsByUsernameAndIdNot("NewName", 1L)).thenReturn(false);

        UpdateMeRequest req = new UpdateMeRequest(null, "  NewName  ", null);

        // Act
        boolean changed = userService.updateMe(1L, req);

        // Assert
        assertThat(changed).isTrue();
        verify(u).setUsername("NewName");
        verify(userRepository).save(u);
    }

    @Test
    @DisplayName("updateMe: met à jour le password (policy ok) et save le hash encodé")
    void updateMe_updatesPassword_andSavesEncodedHash() {
        // Arrange
        User u = mock(User.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        // Doit respecter la policy du service: >=8 + lower/upper/digit/special
        String raw = "Abcdef1!";
        when(passwordEncoder.encode(raw)).thenReturn("ENC");

        UpdateMeRequest req = new UpdateMeRequest(null, null, raw);

        // Act
        boolean changed = userService.updateMe(1L, req);

        // Assert
        assertThat(changed).isTrue();
        verify(passwordEncoder).encode(raw);
        verify(u).setPasswordHash("ENC");
        verify(userRepository).save(u);
    }

    @Test
    @DisplayName("updateMe: mappe DataIntegrityViolationException en 409")
    void updateMe_mapsDataIntegrityViolation_toConflict() {
        // Arrange
        User u = mock(User.class);
        when(u.getId()).thenReturn(1L);
        when(u.getEmail()).thenReturn("old@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        when(userRepository.existsByEmailAndIdNot("new@example.com", 1L)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("dup")).when(userRepository).save(u);

        UpdateMeRequest req = new UpdateMeRequest("new@example.com", null, null);

        // Act + Assert
        assertThatThrownBy(() -> userService.updateMe(1L, req))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("Email or username already used");
    }
}
