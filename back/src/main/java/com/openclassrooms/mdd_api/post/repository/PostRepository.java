package com.openclassrooms.mdd_api.post.repository;

import com.openclassrooms.mdd_api.post.entity.Post;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Feed: récupérer les posts des topics abonnés.
     * EntityGraph charge topic + author pour éviter le N+1 au mapping DTO.
     */
    @EntityGraph(attributePaths = {"topic", "author"})
    List<Post> findByTopic_IdIn(Collection<Long> topicIds, Sort sort);
}
