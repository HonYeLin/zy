package com.pawtrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "animals")
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name; // 给小动物起的名字

    @Column(length = 50)
    private String breed; // 品种/特征描述，例如 Cat, Dog, Other

    @Column(name = "description", length = 500)
    private String description; // AI 优化整合后的特征描述

    @Column(name = "qr_code_id", unique = true, length = 100)
    private String qrCodeId; // 对应线下扫描二维码的唯一标识

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl; // AI 自动选择的代表性头像照片

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary; // AI 生成的简介

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt; // 档案建立时间
}
