package com.openclassrooms.mdd_api.subscription.entity;

import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "subscriptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_subscriptions_user_topic",
                columnNames = {"user_id", "topic_id"}
        ),
        indexes = {
                @Index(name = "idx_subscriptions_user_id", columnList = "user_id"),
                @Index(name = "idx_subscriptions_topic_id", columnList = "topic_id")
        }
)
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK -> users.id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // FK -> topics.id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;


    public Subscription(User user, Topic topic) {
        this.user = user;
        this.topic = topic;
    }
}
