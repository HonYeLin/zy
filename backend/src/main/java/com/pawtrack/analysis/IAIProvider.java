package com.pawtrack.analysis;

public interface IAIProvider {
    /**
     * 推理动物可能正在做什么
     * @param context 历史轨迹记录序列 (自然语言文本)
     * @return AI推理结果
     */
    String reasonBehavior(String context);

    /**
     * 根据描述将动物的状态分类为指定的枚举值
     * @param description 描述文本
     * @return 行为标签 (EATING, SLEEPING, PLAYING, SUNBATHING, WALKING, OTHER)
     */
    String classifyBehavior(String description);

    /**
     * 判断新特征是否匹配已有的小动物实体
     * @param type 动物类型 (Cat, Dog, Other)
     * @param newFeatures 新发现特征
     * @param existingAnimals 已有的同类小动物列表
     * @return 匹配的动物ID，若不匹配返回 null
     */
    Long matchExistingAnimal(String type, String newFeatures, java.util.List<com.pawtrack.entity.Animal> existingAnimals);

    /**
     * 整合和优化已保存特征与新观察到的特征
     * @param oldFeatures 已有特征描述
     * @param newFeatures 新观察特征描述
     * @return 优化后的特征描述
     */
    String optimizeAnimalFeatures(String oldFeatures, String newFeatures);

    /**
     * 生成动物档案简介
     * @param animal 动物实体
     * @param logs 动物的所有足迹记录
     * @return AI生成的简介
     */
    String generateAnimalSummary(com.pawtrack.entity.Animal animal, java.util.List<com.pawtrack.entity.LocationLog> logs);
}
