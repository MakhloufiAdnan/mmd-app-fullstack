package com.openclassrooms.mdd_api.support;

import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Test utility to ensure topics exist in DB for integration tests.
 */
public final class TestTopicSeeder {

    private TestTopicSeeder() {
        // utility class
    }

    /**
     * Ensure at least one topic exists. If the table is empty, inserts all provided names.
     */
    public static void ensureTopicsExist(TopicRepository topicRepository, String... names) {
        if (topicRepository.count() > 0) return;

        if (names == null || names.length == 0) {
            throw new IllegalArgumentException("At least one topic name must be provided");
        }

        for (String name : names) {
            topicRepository.save(newTopic(name));
        }
    }

    private static Topic newTopic(String name) {
        try {
            Constructor<Topic> ctor = Topic.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            Topic t = ctor.newInstance();

            // Prefer setter if present
            try {
                Method setName = Topic.class.getMethod("setName", String.class);
                setName.invoke(t, name);
            } catch (NoSuchMethodException ignore) {
                // Fallback: direct field access
                Field f = Topic.class.getDeclaredField("name");
                f.setAccessible(true);
                f.set(t, name);
            }

            return t;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot seed Topic for integration tests", e);
        }
    }
}
