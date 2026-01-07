package com.openclassrooms.mdd_api.feed.dto;

import java.time.Instant;

public record FeedItemDto(
        Long id,
        FeedTopicDto topic,
        String title,
        String content,
        FeedAuthorDto author,
        Instant createdAt,
        long commentsCount
) {}
