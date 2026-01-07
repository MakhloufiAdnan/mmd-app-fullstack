package com.openclassrooms.mdd_api.topic.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "topics",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_topics_name", columnNames = "name")
        }
)
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    public Topic(String name,  String description) {

        this.name = name;
        this.description = description;
    }
}
