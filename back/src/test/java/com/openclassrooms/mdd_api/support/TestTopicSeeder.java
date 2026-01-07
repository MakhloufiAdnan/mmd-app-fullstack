package com.openclassrooms.mdd_api.support;

import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class TestTopicSeeder {

    private TestTopicSeeder() {}

    public static void ensureTopicsExist(TopicRepository topicRepository, String... names) {
        Set<String> existing = new HashSet<>(
                topicRepository.findAll().stream()
                        .map(Topic::getName)
                        .toList()
        );

        Arrays.stream(names)
                .filter(n -> !existing.contains(n))
                .forEach(n -> topicRepository.save(
                        new Topic(n, defaultDescription(n))
                ));
    }

    private static String defaultDescription(String name) {
        return "Description " + name + "\nLigne 2\nLigne 3";
    }
}
