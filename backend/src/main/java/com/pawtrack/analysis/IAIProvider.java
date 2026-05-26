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
}
