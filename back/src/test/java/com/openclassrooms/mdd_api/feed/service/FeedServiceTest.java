package com.openclassrooms.mdd_api.feed.service;

import com.openclassrooms.mdd_api.comment.repository.CommentRepository;
import com.openclassrooms.mdd_api.comment.repository.PostCommentCountRow;
import com.openclassrooms.mdd_api.feed.dto.FeedAuthorDto;
import com.openclassrooms.mdd_api.feed.dto.FeedItemDto;
import com.openclassrooms.mdd_api.feed.dto.FeedTopicDto;
import com.openclassrooms.mdd_api.post.entity.Post;
import com.openclassrooms.mdd_api.post.repository.PostRepository;
import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FeedService}.
 */
@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;

    @InjectMocks FeedService feedService;

    @Test
    @DisplayName("getFeed: retourne vide si l'utilisateur n'a aucun abonnement (null)")
    void getFeed_returnsEmpty_whenNoSubscriptionsNull() {
        // Arrange
        when(subscriptionRepository.findTopicIdsByUserId(1L)).thenReturn(null);

        // Act
        List<FeedItemDto> res = feedService.getFeed(1L, "desc", null);

        // Assert
        assertThat(res).isEmpty();
        verify(subscriptionRepository).findTopicIdsByUserId(1L);
        verifyNoInteractions(postRepository, commentRepository);
    }

    @Test
    @DisplayName("getFeed: retourne vide si l'utilisateur n'a aucun abonnement (liste vide)")
    void getFeed_returnsEmpty_whenNoSubscriptionsEmpty() {
        // Arrange
        when(subscriptionRepository.findTopicIdsByUserId(1L)).thenReturn(List.of());

        // Act
        List<FeedItemDto> res = feedService.getFeed(1L, "desc", null);

        // Assert
        assertThat(res).isEmpty();
        verify(subscriptionRepository).findTopicIdsByUserId(1L);
        verifyNoInteractions(postRepository, commentRepository);
    }

    @Test
    @DisplayName("getFeed: retourne vide si topicId fourni mais l'utilisateur n'est pas abonné à ce topic")
    void getFeed_returnsEmpty_whenTopicFilterNotSubscribed() {
        // Arrange
        when(subscriptionRepository.findTopicIdsByUserId(1L)).thenReturn(List.of(10L, 11L));

        // Act
        List<FeedItemDto> res = feedService.getFeed(1L, "desc", 99L);

        // Assert
        assertThat(res).isEmpty();
        verify(subscriptionRepository).findTopicIdsByUserId(1L);
        verifyNoInteractions(postRepository, commentRepository);
    }

    @Test
    @DisplayName("getFeed: si topicId fourni et abonné -> requête posts sur le seul topicId")
    void getFeed_filtersOnSingleTopicId_whenTopicFilterSubscribed() {
        // Arrange
        when(subscriptionRepository.findTopicIdsByUserId(1L)).thenReturn(List.of(10L, 11L));
        when(postRepository.findByTopic_IdIn(eq(List.of(11L)), any(Sort.class))).thenReturn(List.of());

        // Act
        List<FeedItemDto> res = feedService.getFeed(1L, "desc", 11L);

        // Assert
        assertThat(res).isEmpty();
        verify(postRepository).findByTopic_IdIn(eq(List.of(11L)), any(Sort.class));
        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("getFeed: si aucun post -> ne lance pas la requête de comptage commentaires")
    void getFeed_returnsEmpty_whenNoPosts() {
        // Arrange
        when(subscriptionRepository.findTopicIdsByUserId(1L)).thenReturn(List.of(10L));
        when(postRepository.findByTopic_IdIn(eq(List.of(10L)), any(Sort.class))).thenReturn(List.of());

        // Act
        List<FeedItemDto> res = feedService.getFeed(1L, "desc", null);

        // Assert
        assertThat(res).isEmpty();
        verify(postRepository).findByTopic_IdIn(eq(List.of(10L)), any(Sort.class));
        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("getFeed: mappe les posts et met commentsCount=0 si absent des résultats agrégés")
    void getFeed_mapsPostsAndDefaultsMissingCountsToZero() {
        // Arrange
        when(subscriptionRepository.findTopicIdsByUserId(1L)).thenReturn(List.of(10L));

        Instant i1 = Instant.parse("2025-01-01T00:00:00Z");
        Instant i2 = Instant.parse("2025-01-02T00:00:00Z");

        // Post #1
        Post p1 = mock(Post.class);
        Topic topic = mock(Topic.class);
        User author1 = mock(User.class);

        when(p1.getId()).thenReturn(1L);
        when(p1.getTitle()).thenReturn("T1");
        when(p1.getContent()).thenReturn("C1");
        when(p1.getCreatedAt()).thenReturn(i1);
        when(p1.getTopic()).thenReturn(topic);
        when(topic.getId()).thenReturn(10L);
        when(topic.getName()).thenReturn("Java");
        when(p1.getAuthor()).thenReturn(author1);
        when(author1.getId()).thenReturn(7L);
        when(author1.getUsername()).thenReturn("alice");

        // Post #2
        Post p2 = mock(Post.class);
        User author2 = mock(User.class);

        when(p2.getId()).thenReturn(2L);
        when(p2.getTitle()).thenReturn("T2");
        when(p2.getContent()).thenReturn("C2");
        when(p2.getCreatedAt()).thenReturn(i2);
        when(p2.getTopic()).thenReturn(topic);
        when(p2.getAuthor()).thenReturn(author2);
        when(author2.getId()).thenReturn(8L);
        when(author2.getUsername()).thenReturn("bob");

        when(postRepository.findByTopic_IdIn(eq(List.of(10L)), any(Sort.class))).thenReturn(List.of(p1, p2));

        // Count uniquement pour p1
        PostCommentCountRow row = mock(PostCommentCountRow.class);
        when(row.getPostId()).thenReturn(1L);
        when(row.getCount()).thenReturn(5L);

        when(commentRepository.countByPostIds(anyList())).thenReturn(List.of(row));

        // Act
        List<FeedItemDto> res = feedService.getFeed(1L, "desc", null);

        // Assert
        List<FeedItemDto> expected = List.of(
                new FeedItemDto(
                        1L,
                        new FeedTopicDto(10L, "Java"),
                        "T1",
                        "C1",
                        new FeedAuthorDto(7L, "alice"),
                        i1,
                        5L
                ),
                new FeedItemDto(
                        2L,
                        new FeedTopicDto(10L, "Java"),
                        "T2",
                        "C2",
                        new FeedAuthorDto(8L, "bob"),
                        i2,
                        0L // absent du Map => 0 par défaut
                )
        );

        assertThat(res).containsExactlyElementsOf(expected);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> idsCaptor = (ArgumentCaptor<List<Long>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(commentRepository).countByPostIds(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("getFeed: si countByPostIds renvoie vide -> counts par défaut à 0")
    void getFeed_defaultsCountsToZero_whenNoRowsReturned() {
        // Arrange
        when(subscriptionRepository.findTopicIdsByUserId(1L)).thenReturn(List.of(10L));

        Instant i1 = Instant.parse("2025-01-01T00:00:00Z");

        Post p1 = mock(Post.class);
        Topic topic = mock(Topic.class);
        User author = mock(User.class);

        when(p1.getId()).thenReturn(1L);
        when(p1.getTitle()).thenReturn("T1");
        when(p1.getContent()).thenReturn("C1");
        when(p1.getCreatedAt()).thenReturn(i1);
        when(p1.getTopic()).thenReturn(topic);
        when(topic.getId()).thenReturn(10L);
        when(topic.getName()).thenReturn("Java");
        when(p1.getAuthor()).thenReturn(author);
        when(author.getId()).thenReturn(7L);
        when(author.getUsername()).thenReturn("alice");

        when(postRepository.findByTopic_IdIn(eq(List.of(10L)), any(Sort.class))).thenReturn(List.of(p1));
        when(commentRepository.countByPostIds(anyList())).thenReturn(List.of());

        // Act
        List<FeedItemDto> res = feedService.getFeed(1L, "desc", null);

        // Assert
        assertThat(res).containsExactly(
                new FeedItemDto(
                        1L,
                        new FeedTopicDto(10L, "Java"),
                        "T1",
                        "C1",
                        new FeedAuthorDto(7L, "alice"),
                        i1,
                        0L
                )
        );
    }

    @ParameterizedTest(name = "order={0} -> Sort ASC")
    @ValueSource(strings = {"asc", "ASC"})
    void getFeed_buildsSortAsc(String order) {
        // Arrange
        when(subscriptionRepository.findTopicIdsByUserId(1L)).thenReturn(List.of(10L));
        when(postRepository.findByTopic_IdIn(eq(List.of(10L)), any(Sort.class))).thenReturn(List.of());

        // Act
        feedService.getFeed(1L, order, null);

        // Assert
        ArgumentCaptor<Sort> captor = ArgumentCaptor.forClass(Sort.class);
        verify(postRepository).findByTopic_IdIn(eq(List.of(10L)), captor.capture());

        Sort sort = captor.getValue();
        Sort.Order createdAtOrder = sort.getOrderFor("createdAt");
        assertThat(createdAtOrder).isNotNull();
        assertThat(createdAtOrder.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @ParameterizedTest(name = "order={0} -> Sort DESC (default)")
    @NullSource
    @ValueSource(strings = {"desc", "DESC", "whatever"})
    void getFeed_buildsSortDescByDefault(String order) {
        // Arrange
        when(subscriptionRepository.findTopicIdsByUserId(1L)).thenReturn(List.of(10L));
        when(postRepository.findByTopic_IdIn(eq(List.of(10L)), any(Sort.class))).thenReturn(List.of());

        // Act
        feedService.getFeed(1L, order, null);

        // Assert
        ArgumentCaptor<Sort> captor = ArgumentCaptor.forClass(Sort.class);
        verify(postRepository).findByTopic_IdIn(eq(List.of(10L)), captor.capture());

        Sort sort = captor.getValue();
        Sort.Order createdAtOrder = sort.getOrderFor("createdAt");
        assertThat(createdAtOrder).isNotNull();
        assertThat(createdAtOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
