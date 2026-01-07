package com.openclassrooms.mdd_api.topic.dto;

/**
 * Représentation d'un topic (id + name).
 */
public record TopicDto(
        Long id,
        String name
) {}
