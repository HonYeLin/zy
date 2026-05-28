package com.pawtrack.dto;

import lombok.Data;

@Data
public class RatingRequest {
    private Long animalId;
    private Long userId;
    private Integer appearanceScore;
    private Integer temperScore;
    private Integer visibilityScore;
    private Integer clinginessScore;
}
