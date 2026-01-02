package com.openclassrooms.mdd_api.comment.repository;

/**
 * Projection pour la requête groupée count(comments) par postId.
 */
public interface PostCommentCountRow {
    Long getPostId();
    long getCount();
}
