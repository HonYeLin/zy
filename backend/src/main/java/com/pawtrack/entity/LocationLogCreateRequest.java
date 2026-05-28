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
    private String behaviorTag; // EATING | SLEEPING | PLAYING | SUNBATHING | WALKING | OTHER
    private String sceneDescription; // 当前行为画面描述
    private String qrCodeId;
    private Integer timeOffset; // 时间偏移量（以分钟为单位），如：0代表刚才，10代表10分钟前
    private Boolean skipAiMatch; // 是否跳过后台二次 AI 匹配 (如果上传流程已做过去重，可置为 true)
}

