package com.openclassrooms.mdd_api.topic.service;

import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.topic.dto.TopicListItemDto;
import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TopicService}.
 */
@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

    @Mock TopicRepository topicRepository;
    @Mock SubscriptionRepository subscriptionRepository;

    @InjectMocks TopicService topicService;

    @Test
    @DisplayName("listTopics: demande les topics triés par name ASC et marque subscribed correctement")
    void listTopics_sortsAndFlagsSubscribed() {
        // Arrange
        Topic t1 = mock(Topic.class);
        when(t1.getId()).thenReturn(1L);
        when(t1.getName()).thenReturn("Java");
        when(t1.getDescription()).thenReturn("Desc Java");

        Topic t2 = mock(Topic.class);
        when(t2.getId()).thenReturn(2L);
        when(t2.getName()).thenReturn("Spring");
        when(t2.getDescription()).thenReturn("Desc Spring");

        when(topicRepository.findAll(any(Sort.class))).thenReturn(List.of(t1, t2));
        when(subscriptionRepository.findTopicIdsByUserId(99L)).thenReturn(List.of(2L));

        // Act
        List<TopicListItemDto> res = topicService.listTopics(99L);

        // Assert
        // 1) Vérifie le tri demandé au repository (name ASC)
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(topicRepository).findAll(sortCaptor.capture());

        Sort sort = sortCaptor.getValue();
        Sort.Order nameOrder = sort.getOrderFor("name");

        assertThat(nameOrder).isNotNull();
        assertThat(nameOrder.getDirection()).isEqualTo(Sort.Direction.ASC);

        // 2) Vérifie le mapping + subscribed
        assertThat(res).containsExactly(
                new TopicListItemDto(1L, "Java", "Desc Java", false),
                new TopicListItemDto(2L, "Spring", "Desc Spring", true)
        );
    }

    @Test
    @DisplayName("listTopics: si aucune subscription -> tout subscribed=false")
    void listTopics_allUnsubscribed_whenNoSubscriptions() {
        // Arrange
        Topic topic = mock(Topic.class);
        when(topic.getId()).thenReturn(1L);
        when(topic.getName()).thenReturn("Java");
        when(topic.getDescription()).thenReturn("Desc Java");

        when(topicRepository.findAll(any(Sort.class))).thenReturn(List.of(topic));
        when(subscriptionRepository.findTopicIdsByUserId(99L)).thenReturn(List.of());

        // Act
        List<TopicListItemDto> res = topicService.listTopics(99L);

        // Assert
        assertThat(res).containsExactly(
                new TopicListItemDto(1L, "Java", "Desc Java", false)
        );
    }
}
