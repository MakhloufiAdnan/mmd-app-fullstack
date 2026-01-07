package com.openclassrooms.mdd_api.subscription.repository;

import com.openclassrooms.mdd_api.subscription.entity.Subscription;
import com.openclassrooms.mdd_api.topic.dto.TopicDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByUser_IdAndTopic_Id(Long userId, Long topicId);

    long deleteByUser_IdAndTopic_Id(Long userId, Long topicId);


    @Query("select s.topic.id from Subscription s where s.user.id = :userId")
    List<Long> findTopicIdsByUserId(@Param("userId") Long userId);

    @Query("""
        select new com.openclassrooms.mdd_api.topic.dto.TopicDto(t.id, t.name)
        from Subscription s
        join s.topic t
        where s.user.id = :userId
        order by t.name asc
    """)
    List<TopicDto> findSubscribedTopicsByUserId(@Param("userId") Long userId);
}
