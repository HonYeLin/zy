-- 创建数据库
CREATE DATABASE IF NOT EXISTS campus_paw_track 
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE campus_paw_track;

-- 1. 动物档案表 (记录猫咪的基本信息)
CREATE TABLE animals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '给小动物起的名字',
    breed VARCHAR(50) COMMENT '品种/特征描述',
    qr_code_id VARCHAR(100) UNIQUE COMMENT '对应线下扫描二维码的唯一标识',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '档案建立时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 观察记录表 (最核心的业务表)
CREATE TABLE animal_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    animal_id BIGINT NOT NULL COMMENT '关联动物ID',
    user_id BIGINT COMMENT '记录者ID (可为空，支持游客模式)',
    location POINT NOT NULL SRID 4326 COMMENT '地理坐标(经纬度)',
    behavior_tag ENUM('EATING', 'SLEEPING', 'PLAYING', 'SUNBATHING', 'WALKING', 'OTHER') NOT NULL COMMENT '行为标记',
    photo_url VARCHAR(255) COMMENT '现场照片路径',
    description VARCHAR(200) COMMENT '补充描述',
    recorded_at DATETIME NOT NULL COMMENT '实际拍摄/观察时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '数据录入时间',
    
    -- 创建空间索引，极大提升半径查询性能
    SPATIAL INDEX (location),
    FOREIGN KEY (animal_id) REFERENCES animals(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. AI 推理结果表 (记录预测行为)
CREATE TABLE ai_predictions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    animal_id BIGINT NOT NULL,
    predicted_behavior VARCHAR(50) NOT NULL,
    confidence_score DECIMAL(5, 4) COMMENT '预测置信度',
    predicted_at DATETIME COMMENT '预测生效时间',
    reasoning_text TEXT COMMENT 'AI 推理的逻辑过程描述',
    FOREIGN KEY (animal_id) REFERENCES animals(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;