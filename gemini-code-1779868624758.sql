-- 创建 AI 行为叙事记录表
CREATE TABLE animal_life_narratives (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    animal_id BIGINT NOT NULL COMMENT '关联动物ID',
    
    -- 叙事核心内容
    narrative_content TEXT NOT NULL COMMENT 'AI生成的行为描述(如：在教学楼A睡觉...)',
    
    -- 范围定义
    start_time DATETIME NOT NULL COMMENT '该叙事覆盖的时间区间开始',
    end_time DATETIME NOT NULL COMMENT '该叙事覆盖的时间区间结束',
    
    -- 元数据
    summary_type VARCHAR(20) DEFAULT 'DAILY' COMMENT '类型: DAILY, WEEKLY, INSIGHT',
    model_version VARCHAR(50) COMMENT '生成该内容的AI模型版本(方便后续优化比较)',
    token_usage INT COMMENT '记录消耗的Token数(方便统计成本)',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    
    -- 索引优化：方便根据动物ID和时间快速查询“生活史”
    INDEX idx_animal_time (animal_id, start_time, end_time),
    FOREIGN KEY (animal_id) REFERENCES animals(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;