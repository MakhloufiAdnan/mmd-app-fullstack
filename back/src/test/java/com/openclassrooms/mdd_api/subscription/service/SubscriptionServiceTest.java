package com.openclassrooms.mdd_api.subscription.service;

import com.openclassrooms.mdd_api.common.web.exception.ApiConflictException;
import com.openclassrooms.mdd_api.common.web.exception.ApiNotFoundException;
import com.openclassrooms.mdd_api.subscription.entity.Subscription;
import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SubscriptionService}.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock UserRepository userRepository;
    @Mock TopicRepository topicRepository;

    @InjectMocks SubscriptionService subscriptionService;

    @Test
    @DisplayName("subscribe: 404 si topic inexistant")
    void subscribe_throwsNotFound_whenTopicMissing() {
        // Arrange
        when(topicRepository.existsById(10L)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> subscriptionService.subscribe(1L, 10L))
                .isInstanceOf(ApiNotFoundException.class)
                .hasMessageContaining("Topic not found");

        verify(topicRepository).existsById(10L);
        verifyNoInteractions(subscriptionRepository, userRepository);
    }

    @Test
    @DisplayName("subscribe: 409 si déjà abonné (UX check)")
    void subscribe_throwsConflict_whenAlreadySubscribed() {
        // Arrange
        when(topicRepository.existsById(10L)).thenReturn(true);
        when(subscriptionRepository.existsByUser_IdAndTopic_Id(1L, 10L)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> subscriptionService.subscribe(1L, 10L))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("Already subscribed");

        verify(topicRepository).existsById(10L);
        verify(subscriptionRepository).existsByUser_IdAndTopic_Id(1L, 10L);
        verify(userRepository, never()).getReferenceById(anyLong());
        verify(topicRepository, never()).getReferenceById(anyLong());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("subscribe: enregistre Subscription via références JPA et retourne topicId")
    void subscribe_happyPath_savesAndReturnsTopicId() {
        // Arrange
        when(topicRepository.existsById(10L)).thenReturn(true);
        when(subscriptionRepository.existsByUser_IdAndTopic_Id(1L, 10L)).thenReturn(false);

        User userRef = mock(User.class);
        Topic topicRef = mock(Topic.class);
        when(userRepository.getReferenceById(1L)).thenReturn(userRef);
        when(topicRepository.getReferenceById(10L)).thenReturn(topicRef);

        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Long res = subscriptionService.subscribe(1L, 10L);

        // Assert
        assertThat(res).isEqualTo(10L);
        verify(userRepository).getReferenceById(1L);
        verify(topicRepository).getReferenceById(10L);
        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    @DisplayName("subscribe: mappe une contrainte unique DB (concurrence) en 409")
    void subscribe_mapsDataIntegrityViolation_toConflict() {
        // Arrange
        when(topicRepository.existsById(10L)).thenReturn(true);
        when(subscriptionRepository.existsByUser_IdAndTopic_Id(1L, 10L)).thenReturn(false);

        when(userRepository.getReferenceById(1L)).thenReturn(mock(User.class));
        when(topicRepository.getReferenceById(10L)).thenReturn(mock(Topic.class));

        when(subscriptionRepository.save(any(Subscription.class)))
                .thenThrow(new DataIntegrityViolationException("dup"));

        // Act + Assert
        assertThatThrownBy(() -> subscriptionService.subscribe(1L, 10L))
                .isInstanceOf(ApiConflictException.class)
                .hasMessageContaining("Already subscribed");
    }

    @Test
    @DisplayName("unsubscribe: idempotent (appelle delete même si déjà désabonné)")
    void unsubscribe_isIdempotent() {

        // Arrange/Act
        subscriptionService.unsubscribe(1L, 10L);

        // Assert
        verify(subscriptionRepository).deleteByUser_IdAndTopic_Id(1L, 10L);
        verifyNoMoreInteractions(subscriptionRepository);
    }
}
