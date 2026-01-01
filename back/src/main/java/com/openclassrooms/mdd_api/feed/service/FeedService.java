package com.openclassrooms.mdd_api.feed.service;

import com.openclassrooms.mdd_api.comment.repository.CommentRepository;
import com.openclassrooms.mdd_api.comment.repository.PostCommentCountRow;
import com.openclassrooms.mdd_api.feed.dto.FeedAuthorDto;
import com.openclassrooms.mdd_api.feed.dto.FeedItemDto;
import com.openclassrooms.mdd_api.feed.dto.FeedTopicDto;
import com.openclassrooms.mdd_api.post.entity.Post;
import com.openclassrooms.mdd_api.post.repository.PostRepository;
import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final SubscriptionRepository subscriptionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public List<FeedItemDto> getFeed(Long userId, String order, Long topicId) {

        // 1) récupérer topicIds abonnés
        List<Long> topicIds = subscriptionRepository.findTopicIdsByUserId(userId);

        // si aucun abonnement -> feed vide
        if (topicIds == null || topicIds.isEmpty()) {
            return List.of();
        }

        // 2) topicId optionnel : si fourni mais pas abonné -> feed vide
        if (topicId != null) {
            if (!topicIds.contains(topicId)) {
                return List.of();
            }
            topicIds = List.of(topicId);
        }

        // 3) build Sort (createdAt, default DESC)
        Sort sort = buildSort(order);

        // 4) récupérer posts (EntityGraph dans PostRepository évite le N+1 sur topic/author)
        List<Post> posts = postRepository.findByTopic_IdIn(topicIds, sort);

        // Pas de posts : pas besoin de faire la requête counts
        if (posts.isEmpty()) {
            return List.of();
        }

        // 5) récupérer counts groupés
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        List<PostCommentCountRow> rows = commentRepository.countByPostIds(postIds);

        Map<Long, Long> countsByPostId = rows.stream()
                .collect(Collectors.toMap(PostCommentCountRow::getPostId, row -> row.getCount()));

        // 6) mapper vers FeedItemDto
        return posts.stream()
                .map(p -> new FeedItemDto(
                        p.getId(),
                        new FeedTopicDto(p.getTopic().getId(), p.getTopic().getName()),
                        p.getTitle(),
                        new FeedAuthorDto(p.getAuthor().getId(), p.getAuthor().getUsername()),
                        p.getCreatedAt(),
                        countsByPostId.getOrDefault(p.getId(), 0L) // default 0 si absent
                ))
                .toList();
    }

    private static Sort buildSort(String order) {
        Sort.Direction dir = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, "createdAt");
    }
}
