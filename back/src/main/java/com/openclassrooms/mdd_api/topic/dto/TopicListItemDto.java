package com.openclassrooms.mdd_api.topic.dto;

public record TopicListItemDto(
        Long id,
        String name,
        String description,
        boolean subscribed
) {}
