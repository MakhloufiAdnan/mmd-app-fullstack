package com.openclassrooms.mdd_api.post.service;

import com.openclassrooms.mdd_api.comment.entity.Comment;
import com.openclassrooms.mdd_api.comment.repository.CommentRepository;
import com.openclassrooms.mdd_api.common.web.exception.ApiNotFoundException;
import com.openclassrooms.mdd_api.post.dto.CreatePostRequest;
import com.openclassrooms.mdd_api.post.dto.PostAuthorDto;
import com.openclassrooms.mdd_api.post.dto.PostCommentDto;
import com.openclassrooms.mdd_api.post.dto.PostDetailResponse;
import com.openclassrooms.mdd_api.post.dto.PostTopicDto;
import com.openclassrooms.mdd_api.post.entity.Post;
import com.openclassrooms.mdd_api.post.repository.PostRepository;
import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service "Post" : création + lecture détail.
 * - Lecture (detail) : autorisée à tout utilisateur authentifié.
 * - Écriture (create) : nécessite d'être abonné au topic.
 * 403 métier : lever AccessDeniedException.
 * 404 : lever ApiNotFoundException (mappé en 404 par RestExceptionHandler).
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private static final String FORBIDDEN = "Forbidden";

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Crée un post (nécessite abonnement au topic).
     *
     * @return id du post créé
     */
    @Transactional
    public Long createPost(Long userId, CreatePostRequest req) {
        // 1) Charger Topic (404 si absent)
        Topic topic = topicRepository.findById(req.topicId())
                .orElseThrow(() -> new ApiNotFoundException("Topic not found"));

        // 2) Check abonnement (write requires subscription)
        boolean subscribed = subscriptionRepository.existsByUser_IdAndTopic_Id(userId, topic.getId());
        if (!subscribed) {
            throw new AccessDeniedException(FORBIDDEN);
        }

        // 3) Charger User (auteur) (404 si absent : cas "data corrompue" en pratique)
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ApiNotFoundException("User not found"));

        // 4) Construire + sauvegarder (createdAt généré côté back via @PrePersist)
        Post post = new Post(req.title(), req.content(), topic, author);
        Post saved = postRepository.save(post);

        // 5) Retourner l'id
        return saved.getId();
    }

    /**
     * Détail d'un post (lecture autorisée à tout authentifié).
     */
    @Transactional(readOnly = true)
    public PostDetailResponse getPostDetail(Long postId) {
        // 1) Charger Post + topic + author (EntityGraph)
        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new ApiNotFoundException("Post not found"));

        // 2) Charger comments triés desc (createdAt desc, id desc) + author (EntityGraph)
        List<Comment> comments = commentRepository.findByPost_IdOrderByCreatedAtDescIdDesc(postId);

        // 3) Mapper vers DTO contractuel
        PostTopicDto topicDto = new PostTopicDto(post.getTopic().getId(), post.getTopic().getName());
        PostAuthorDto authorDto = new PostAuthorDto(post.getAuthor().getId(), post.getAuthor().getUsername());

        List<PostCommentDto> commentDtos = comments.stream()
                .map(c -> new PostCommentDto(
                        c.getId(),
                        c.getContent(),
                        new PostAuthorDto(c.getAuthor().getId(), c.getAuthor().getUsername()),
                        c.getCreatedAt()
                ))
                .toList();

        return new PostDetailResponse(
                post.getId(),
                topicDto,
                post.getTitle(),
                post.getContent(),
                authorDto,
                post.getCreatedAt(),
                commentDtos
        );
    }
}
