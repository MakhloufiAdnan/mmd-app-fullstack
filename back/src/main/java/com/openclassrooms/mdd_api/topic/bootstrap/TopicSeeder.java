package com.openclassrooms.mdd_api.topic.bootstrap;

import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TopicSeeder implements ApplicationRunner {

    private final TopicRepository topicRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (topicRepository.count() > 0) {
            return;
        }

        topicRepository.saveAll(List.of(
                new Topic("Découvrir le monde formidable de Java"),
                new Topic("Comment maitriser Java"),
                new Topic("Java, par ou commencer")
        ));
    }
}
