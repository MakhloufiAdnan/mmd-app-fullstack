package com.openclassrooms.mdd_api.comment.service;

import com.openclassrooms.mdd_api.comment.dto.CreateCommentRequest;
import com.openclassrooms.mdd_api.comment.entity.Comment;
import com.openclassrooms.mdd_api.comment.repository.CommentRepository;
import com.openclassrooms.mdd_api.common.web.exception.ApiNotFoundException;
import com.openclassrooms.mdd_api.post.entity.Post;
import com.openclassrooms.mdd_api.post.repository.PostRepository;
import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CommentService}.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock SubscriptionRepository subscriptionRepository;

    @InjectMocks CommentService commentService;

    @Test
    void addComment_postNotFound_throws404() {
        // Arrange
        long userId = 1L;
        long postId = 10L;

        CreateCommentRequest req = new CreateCommentRequest("hi"); 
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // Act + Assert (lambda = une seule invocation)
        assertThatThrownBy(() -> commentService.addComment(userId, postId, req))
                .isInstanceOf(ApiNotFoundException.class);

        // Assert (interactions)
        verify(postRepository).findById(postId);
        verifyNoInteractions(subscriptionRepository, userRepository, commentRepository);
    }

    @Test
    void addComment_notSubscribed_throws403() {
        // Arrange
        long userId = 1L;
        long postId = 10L;
        long topicId = 99L;

        CreateCommentRequest req = new CreateCommentRequest("hi");

        Topic topic = mock(Topic.class);
        when(topic.getId()).thenReturn(topicId);

        Post post = mock(Post.class);
        when(post.getTopic()).thenReturn(topic);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(subscriptionRepository.existsByUser_IdAndTopic_Id(userId, topicId)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> commentService.addComment(userId, postId, req))
                .isInstanceOf(AccessDeniedException.class);

        // Assert (interactions)
        verify(postRepository).findById(postId);
        verify(subscriptionRepository).existsByUser_IdAndTopic_Id(userId, topicId);
        verifyNoInteractions(userRepository, commentRepository);
    }

    @Test
    void addComment_userNotFound_throws404() {
        // Arrange
        long userId = 1L;
        long postId = 10L;
        long topicId = 99L;

        CreateCommentRequest req = new CreateCommentRequest("hi"); 

        Topic topic = mock(Topic.class);
        when(topic.getId()).thenReturn(topicId);

        Post post = mock(Post.class);
        when(post.getTopic()).thenReturn(topic);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(subscriptionRepository.existsByUser_IdAndTopic_Id(userId, topicId)).thenReturn(true);

        // User absent en DB => 404
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> commentService.addComment(userId, postId, req))
                .isInstanceOf(ApiNotFoundException.class);

        // Assert (interactions)
        verify(postRepository).findById(postId);
        verify(subscriptionRepository).existsByUser_IdAndTopic_Id(userId, topicId);
        verify(userRepository).findById(userId);
        verifyNoInteractions(commentRepository);
    }

    @Test
    void addComment_ok_savesAndReturnsId() {
        // Arrange
        long userId = 1L;
        long postId = 10L;
        long topicId = 99L;

        CreateCommentRequest req = new CreateCommentRequest("Hello"); 

        Topic topic = mock(Topic.class);
        when(topic.getId()).thenReturn(topicId);

        Post post = mock(Post.class);
        when(post.getTopic()).thenReturn(topic);

        User author = mock(User.class);

        // Pre-conditions: post existe, user abonné, user existe
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(subscriptionRepository.existsByUser_IdAndTopic_Id(userId, topicId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(author));

        // Save returns entity with id (simule JPA)
        Comment saved = mock(Comment.class);
        when(saved.getId()).thenReturn(55L);
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        // Act
        Long id = commentService.addComment(userId, postId, req);

        // Assert (result)
        assertThat(id).isEqualTo(55L);

        // Assert (on vérifie ce qui a été sauvegardé)
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());

        Comment toSave = captor.getValue();
        assertThat(toSave.getContent()).isEqualTo("Hello");
        assertThat(toSave.getPost()).isSameAs(post);
        assertThat(toSave.getAuthor()).isSameAs(author);

        // Assert (interactions minimales attendues)
        verify(postRepository).findById(postId);
        verify(subscriptionRepository).existsByUser_IdAndTopic_Id(userId, topicId);
        verify(userRepository).findById(userId);
    }
}
