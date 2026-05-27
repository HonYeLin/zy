package com.pawtrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "animal_life_narratives")
public class AnimalLifeNarrative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "animal_id", nullable = false)
    private Long animalId;

    @Column(name = "narrative_content", nullable = false, columnDefinition = "TEXT")
    private String narrativeContent;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "summary_type", length = 20)
    private String summaryType = "DAILY"; // DAILY, WEEKLY, INSIGHT

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
