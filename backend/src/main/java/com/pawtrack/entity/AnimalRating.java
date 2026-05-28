package com.pawtrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "animal_ratings")
@Data
public class AnimalRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "animal_id", nullable = false)
    private Long animalId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "appearance_score", nullable = false)
    private Integer appearanceScore;

    @Column(name = "temper_score", nullable = false)
    private Integer temperScore;

    @Column(name = "visibility_score", nullable = false)
    private Integer visibilityScore;

    @Column(name = "clinginess_score", nullable = false)
    private Integer clinginessScore;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
