package com.pawtrack.entity;

import lombok.Data;

@Data
public class FeedbackRequest {
    private Long animalId;
    private String predictedBehavior;
    private String actualBehavior;
    private String feedbackType; // "CONFIRMED" or "CORRECTED"
}
