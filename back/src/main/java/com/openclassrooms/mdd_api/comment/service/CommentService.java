package com.openclassrooms.mdd_api.comment.service;

import com.openclassrooms.mdd_api.comment.dto.CreateCommentRequest;
import com.openclassrooms.mdd_api.comment.entity.Comment;
import com.openclassrooms.mdd_api.comment.repository.CommentRepository;
import com.openclassrooms.mdd_api.common.web.exception.ApiNotFoundException;
import com.openclassrooms.mdd_api.post.entity.Post;
import com.openclassrooms.mdd_api.post.repository.PostRepository;
import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service "Comment" : ajout d'un commentaire à un post.
 * Ajouter un commentaire : nécessite d'être abonné au topic du post.
 * 403 métier : AccessDeniedException.
 * 404 : ApiNotFoundException (post/user introuvable).
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private static final String FORBIDDEN = "Forbidden";

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Ajoute un commentaire (nécessite abonnement au topic du post).
     *
     * @return id du commentaire créé
     */
    @Transactional
    public Long addComment(Long userId, Long postId, CreateCommentRequest req) {
        // 1) Charger Post (404 si absent)
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiNotFoundException("Post not found"));

        // 2) Check abonnement au topic du post (write requires subscription)
        Long topicId = post.getTopic().getId();
        boolean subscribed = subscriptionRepository.existsByUser_IdAndTopic_Id(userId, topicId);
        if (!subscribed) {
            throw new AccessDeniedException(FORBIDDEN);
        }

        // 3) Charger User (auteur)
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ApiNotFoundException("User not found"));

        // 4) Construire + sauvegarder (createdAt généré côté back via @PrePersist)
        Comment comment = new Comment(req.content(), post, author);
        Comment saved = commentRepository.save(comment);

        // 5) Retourner l'id
        return saved.getId();
    }
}
