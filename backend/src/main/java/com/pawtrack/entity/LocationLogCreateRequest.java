package com.pawtrack.entity;

import lombok.Data;

@Data
public class LocationLogCreateRequest {
    private String type; // "Cat" | "Dog" | "Other"
    private String nickname;
    private String features;
    private Double latitude;
    private Double longitude;
    private String photoUrl;
}
