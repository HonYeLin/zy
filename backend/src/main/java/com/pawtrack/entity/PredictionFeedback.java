package com.pawtrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "prediction_feedbacks")
public class PredictionFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "animal_id", nullable = false)
    private Long animalId;

    @Column(name = "predicted_behavior", nullable = false, length = 50)
    private String predictedBehavior;

    @Column(name = "actual_behavior", nullable = false, length = 50)
    private String actualBehavior;

    @Column(name = "feedback_type", nullable = false, length = 20)
    private String feedbackType; // "CONFIRMED" or "CORRECTED"

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
