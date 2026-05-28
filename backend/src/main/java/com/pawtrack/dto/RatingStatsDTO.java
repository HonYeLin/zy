package com.pawtrack.dto;

import lombok.Data;

@Data
public class RatingStatsDTO {
    private Double appearanceAvg;
    private Double temperAvg;
    private Double visibilityAvg;
    private Double clinginessAvg;
    private Integer totalRatings;
}
