package com.openclassrooms.mdd_api.topic.service;

import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.topic.dto.TopicListItemDto;
import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public List<TopicListItemDto> listTopics(Long userId) {
        // Par ordre name ascendant
        List<Topic> topics = topicRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));

        Set<Long> subscribedIds = new HashSet<>(subscriptionRepository.findTopicIdsByUserId(userId));

        return topics.stream()
                .map(t -> new TopicListItemDto(t.getId(), t.getName(), t.getDescription(), subscribedIds.contains(t.getId())))
                .toList();
    }
}
