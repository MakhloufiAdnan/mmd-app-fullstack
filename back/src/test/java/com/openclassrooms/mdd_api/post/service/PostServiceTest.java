package com.openclassrooms.mdd_api.post.service;

import com.openclassrooms.mdd_api.comment.entity.Comment;
import com.openclassrooms.mdd_api.comment.repository.CommentRepository;
import com.openclassrooms.mdd_api.common.web.exception.ApiNotFoundException;
import com.openclassrooms.mdd_api.post.dto.CreatePostRequest;
import com.openclassrooms.mdd_api.post.dto.PostDetailResponse;
import com.openclassrooms.mdd_api.post.entity.Post;
import com.openclassrooms.mdd_api.post.repository.PostRepository;
import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;
    @Mock TopicRepository topicRepository;
    @Mock UserRepository userRepository;
    @Mock SubscriptionRepository subscriptionRepository;

    @InjectMocks PostService postService;

    @Test
    void createPost_topicNotFound_throws404() {
        // Arrange
        long userId = 1L;
        CreatePostRequest req = new CreatePostRequest(10L, "Title", "Content");

        when(topicRepository.findById(10L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> postService.createPost(userId, req))
                .isInstanceOf(ApiNotFoundException.class);

        verify(topicRepository).findById(10L);
        verifyNoMoreInteractions(topicRepository);
        verifyNoInteractions(subscriptionRepository, userRepository, postRepository);
    }

    @Test
    void createPost_notSubscribed_throws403() {
        // Arrange
        long userId = 1L;
        long topicId = 10L;
        CreatePostRequest req = new CreatePostRequest(topicId, "Title", "Content");

        Topic topic = mock(Topic.class);
        when(topic.getId()).thenReturn(topicId);

        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(subscriptionRepository.existsByUser_IdAndTopic_Id(userId, topicId)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> postService.createPost(userId, req))
                .isInstanceOf(AccessDeniedException.class);

        verify(topicRepository).findById(topicId);
        verify(subscriptionRepository).existsByUser_IdAndTopic_Id(userId, topicId);
        verifyNoInteractions(userRepository, postRepository);
    }

    @Test
    void createPost_userNotFound_throws404() {
        // Arrange
        long userId = 1L;
        long topicId = 10L;
        CreatePostRequest req = new CreatePostRequest(topicId, "Title", "Content");

        Topic topic = mock(Topic.class);
        when(topic.getId()).thenReturn(topicId);

        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(subscriptionRepository.existsByUser_IdAndTopic_Id(userId, topicId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> postService.createPost(userId, req))
                .isInstanceOf(ApiNotFoundException.class);

        verify(topicRepository).findById(topicId);
        verify(subscriptionRepository).existsByUser_IdAndTopic_Id(userId, topicId);
        verify(userRepository).findById(userId);
        verifyNoInteractions(postRepository);
    }

    @Test
    void createPost_ok_savesAndReturnsId() {
        // Arrange
        long userId = 1L;
        long topicId = 10L;
        CreatePostRequest req = new CreatePostRequest(topicId, "Hello", "World");

        Topic topic = mock(Topic.class);
        when(topic.getId()).thenReturn(topicId);

        User author = mock(User.class);

        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(subscriptionRepository.existsByUser_IdAndTopic_Id(userId, topicId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));

        Post saved = mock(Post.class);
        when(saved.getId()).thenReturn(42L);
        when(postRepository.save(any(Post.class))).thenReturn(saved);

        // Act
        Long id = postService.createPost(userId, req);

        // Assert
        assertThat(id).isEqualTo(42L);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());

        Post toSave = captor.getValue();
        assertThat(toSave.getTitle()).isEqualTo("Hello");
        assertThat(toSave.getContent()).isEqualTo("World");
        assertThat(toSave.getTopic()).isSameAs(topic);
        assertThat(toSave.getAuthor()).isSameAs(author);
    }

    @Test
    void getPostDetail_postNotFound_throws404() {
        // Arrange
        long postId = 99L;
        when(postRepository.findDetailById(postId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> postService.getPostDetail(postId))
                .isInstanceOf(ApiNotFoundException.class);

        verify(postRepository).findDetailById(postId);
        verifyNoInteractions(commentRepository);
    }

    @Test
    void getPostDetail_ok_mapsDto() {
        // Arrange
        long postId = 7L;

        Topic topic = mock(Topic.class);
        when(topic.getId()).thenReturn(10L);
        when(topic.getName()).thenReturn("Java");

        User author = mock(User.class);
        when(author.getId()).thenReturn(1L);
        when(author.getUsername()).thenReturn("adnan");

        Post post = mock(Post.class);
        when(post.getId()).thenReturn(postId);
        when(post.getTitle()).thenReturn("T");
        when(post.getContent()).thenReturn("C");
        when(post.getCreatedAt()).thenReturn(Instant.parse("2026-01-05T00:00:00Z"));
        when(post.getTopic()).thenReturn(topic);
        when(post.getAuthor()).thenReturn(author);

        User c1Author = mock(User.class);
        when(c1Author.getId()).thenReturn(2L);
        when(c1Author.getUsername()).thenReturn("bob");

        Comment c1 = mock(Comment.class);
        when(c1.getId()).thenReturn(100L);
        when(c1.getContent()).thenReturn("first");
        when(c1.getCreatedAt()).thenReturn(Instant.parse("2026-01-05T01:00:00Z"));
        when(c1.getAuthor()).thenReturn(c1Author);

        User c2Author = mock(User.class);
        when(c2Author.getId()).thenReturn(3L);
        when(c2Author.getUsername()).thenReturn("alice");

        Comment c2 = mock(Comment.class);
        when(c2.getId()).thenReturn(101L);
        when(c2.getContent()).thenReturn("second");
        when(c2.getCreatedAt()).thenReturn(Instant.parse("2026-01-05T02:00:00Z"));
        when(c2.getAuthor()).thenReturn(c2Author);

        when(postRepository.findDetailById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findByPost_IdOrderByCreatedAtDescIdDesc(postId)).thenReturn(List.of(c2, c1)); // déjà trié repo

        // Act
        PostDetailResponse res = postService.getPostDetail(postId);

        // Assert (chaîne unique)
        assertThat(res)
                .extracting(PostDetailResponse::id, PostDetailResponse::title, PostDetailResponse::content)
                .containsExactly(postId, "T", "C");

        assertThat(res.topic().id()).isEqualTo(10L);
        assertThat(res.topic().name()).isEqualTo("Java");

        assertThat(res.author().id()).isEqualTo(1L);
        assertThat(res.author().username()).isEqualTo("adnan");

        assertThat(res.comments()).hasSize(2);
        assertThat(res.comments().get(0).id()).isEqualTo(101L);
        assertThat(res.comments().get(0).content()).isEqualTo("second");
        assertThat(res.comments().get(0).author().username()).isEqualTo("alice");

        verify(postRepository).findDetailById(postId);
        verify(commentRepository).findByPost_IdOrderByCreatedAtDescIdDesc(postId);
    }
}
