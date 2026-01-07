package com.openclassrooms.mdd_api.subscription.service;

import com.openclassrooms.mdd_api.common.web.exception.ApiConflictException;
import com.openclassrooms.mdd_api.common.web.exception.ApiNotFoundException;
import com.openclassrooms.mdd_api.subscription.entity.Subscription;
import com.openclassrooms.mdd_api.subscription.repository.SubscriptionRepository;
import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.topic.repository.TopicRepository;
import com.openclassrooms.mdd_api.user.entity.User;
import com.openclassrooms.mdd_api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;

    @Transactional
    public Long subscribe(Long userId, Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new ApiNotFoundException("Topic not found");
        }

        // check UX : déjà abonné => 409
        if (subscriptionRepository.existsByUser_IdAndTopic_Id(userId, topicId)) {
            throw new ApiConflictException("Already subscribed");
        }

        // Evite une requête inutile : références JPA
        User userRef = userRepository.getReferenceById(userId);
        Topic topicRef = topicRepository.getReferenceById(topicId);

        try {
            subscriptionRepository.save(new Subscription(userRef, topicRef));
        } catch (DataIntegrityViolationException e) {
            // cas concurrence/double-clic : la contrainte unique DB tranche
            throw new ApiConflictException("Already subscribed");
        }

        return topicId;
    }

    @Transactional
    public void unsubscribe(Long userId, Long topicId) {
        // contrat: 204, même si déjà désabonné
        subscriptionRepository.deleteByUser_IdAndTopic_Id(userId, topicId);
    }
}
