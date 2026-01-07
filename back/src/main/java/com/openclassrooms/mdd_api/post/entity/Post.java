package com.openclassrooms.mdd_api.post.entity;

import com.openclassrooms.mdd_api.topic.entity.Topic;
import com.openclassrooms.mdd_api.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, length = 2000)
    private String content;

    // Généré côté back
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // FK -> topics.id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    // FK -> users.id (auteur)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    public Post(String title, String content, Topic topic, User author) {
        this.title = title;
        this.content = content;
        this.topic = topic;
        this.author = author;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
