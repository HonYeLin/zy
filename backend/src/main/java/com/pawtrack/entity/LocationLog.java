package com.pawtrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Formula;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "animal_logs")
public class LocationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @Column(name = "user_id")
    private Long userId;

    // Use Formula to dynamically read spatial POINT coordinate values
    @Formula("ST_Latitude(location)")
    private Double latitude;

    @Formula("ST_Longitude(location)")
    private Double longitude;

    @Column(name = "behavior_tag", nullable = false)
    @Enumerated(EnumType.STRING)
    private BehaviorTag behaviorTag; // EATING, SLEEPING, PLAYING, etc.

    @Column(name = "photo_url", length = 255)
    private String photoUrl; // 现场照片路径

    @Column(length = 200)
    private String description; // 补充描述 / 特征

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt; // 实际拍摄/观察时间

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt; // 数据录入时间
}
