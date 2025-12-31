package com.openclassrooms.mdd_api.subscription.repository;

import com.openclassrooms.mdd_api.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByUser_IdAndTopic_Id(Long userId, Long topicId);

    long deleteByUser_IdAndTopic_Id(Long userId, Long topicId);

    @Query("select s.topic.id from Subscription s where s.user.id = :userId")
    List<Long> findTopicIdsByUserId(Long userId);
}
