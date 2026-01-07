package com.openclassrooms.mdd_api.comment.repository;

import com.openclassrooms.mdd_api.comment.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
        select c.post.id as postId, count(c.id) as count
        from Comment c
        where c.post.id in :postIds
        group by c.post.id
    """)
    List<PostCommentCountRow> countByPostIds(@Param("postIds") Collection<Long> postIds);

    /**
     * Detail: charge author pour éviter le N+1, et trie de façon déterministe.
     */
    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByPost_IdOrderByCreatedAtDescIdDesc(Long postId);
}
