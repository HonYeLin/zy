package com.pawtrack.analysis;

import com.pawtrack.entity.LocationLog;
import com.pawtrack.repository.LocationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final IAIProvider aiProvider;
    private final LocationLogRepository locationLogRepository;

    /**
     * 根据特定动物的历史记录进行 AI 行为推理
     */
    public String analyzeAnimalBehavior(Long animalId) {
        // 1. 数据清洗：获取最近的历史记录（这里取最近 10 条）
        List<LocationLog> recentLogs = locationLogRepository.findByAnimalIdOrderByRecordedAtDesc(animalId);
        
        if (recentLogs == null || recentLogs.isEmpty()) {
            return "缺乏历史轨迹数据，无法推理。";
        }

        // 取前10条
        int limit = Math.min(recentLogs.size(), 10);
        recentLogs = recentLogs.subList(0, limit);

        // 2. Context 组装
        StringBuilder contextBuilder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        // 按照时间正序构建文本 (因为取出来是倒序的)
        for (int i = recentLogs.size() - 1; i >= 0; i--) {
            LocationLog log = recentLogs.get(i);
            String time = log.getRecordedAt() != null ? log.getRecordedAt().format(formatter) : "未知时间";
            String behavior = log.getBehaviorLabel() != null ? log.getBehaviorLabel() : "活动";
            contextBuilder.append("- 在 ").append(time)
                          .append("，位于经纬度(").append(log.getLongitude()).append(", ").append(log.getLatitude()).append(")，")
                          .append("状态：").append(behavior).append("\n");
        }

        // 3. 调用 AI 接口进行推演
        return aiProvider.reasonBehavior(contextBuilder.toString());
    }
}
