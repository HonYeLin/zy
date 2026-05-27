package com.pawtrack.analysis;

import com.pawtrack.entity.LocationLog;
import com.pawtrack.entity.PredictionFeedback;
import com.pawtrack.entity.AnimalLifeNarrative;
import com.pawtrack.entity.FeedbackRequest;
import com.pawtrack.repository.LocationLogRepository;
import com.pawtrack.repository.PredictionFeedbackRepository;
import com.pawtrack.repository.AnimalLifeNarrativeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final IAIProvider aiProvider;
    private final LocationLogRepository locationLogRepository;
    private final PredictionFeedbackRepository predictionFeedbackRepository;
    private final AnimalLifeNarrativeRepository animalLifeNarrativeRepository;

    /**
     * 根据特定动物的历史记录进行 AI 行为推理，并结合历史纠偏进行 Few-shot 自我微调
     */
    public String analyzeAnimalBehavior(Long animalId) {
        // 1. 数据清理：获取该动物最近 7 天内的足迹记录
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<LocationLog> recentLogs = locationLogRepository.findByAnimalIdAndRecordedAtAfterOrderByRecordedAtDesc(animalId, sevenDaysAgo);
        
        // 若最近 7 天内无数据，则退回拉取该动物最近 10 条（防止数据荒废无法推理）
        if (recentLogs == null || recentLogs.isEmpty()) {
            recentLogs = locationLogRepository.findByAnimalIdOrderByRecordedAtDesc(animalId);
            if (recentLogs == null || recentLogs.isEmpty()) {
                return "缺乏历史轨迹数据，无法推理。";
            }
            int limit = Math.min(recentLogs.size(), 10);
            recentLogs = recentLogs.subList(0, limit);
        }

        // 2. 偏差修正上下文构建 (Few-shot Learning)
        List<PredictionFeedback> corrections = predictionFeedbackRepository.findByAnimalIdAndFeedbackTypeOrderByRecordedAtDesc(animalId, "CORRECTED");
        StringBuilder correctionContext = new StringBuilder();
        if (corrections != null && !corrections.isEmpty()) {
            correctionContext.append("【注意：这是该小动物历史上的人工行为纠偏错误日志，代表了它的个性偏好，请重点根据这些历史纠正来动态修正对它的行为推断权重】：\n");
            int limit = Math.min(corrections.size(), 5); // 考虑最近 5 条纠错样本
            for (int i = 0; i < limit; i++) {
                PredictionFeedback f = corrections.get(i);
                correctionContext.append("- 曾预测它此时最可能在 [").append(f.getPredictedBehavior())
                                  .append("]，但实际上用户修正为 [").append(f.getActualBehavior()).append("]；\n");
            }
            correctionContext.append("\n");
        }

        // 3. 时间自然语言序列拼装
        StringBuilder contextBuilder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        // 按照时间正序构建文本 (因为取出来是倒序的)
        for (int i = recentLogs.size() - 1; i >= 0; i--) {
            LocationLog log = recentLogs.get(i);
            String time = log.getRecordedAt() != null ? log.getRecordedAt().format(formatter) : "未知时间";
            String behavior = log.getBehaviorLabel() != null ? log.getBehaviorLabel() : "活动";
            String placeDesc = (log.getDescription() != null && !log.getDescription().trim().isEmpty()) ? log.getDescription().trim() : "未标记具体地点";
            
            contextBuilder.append("- 在 ").append(time)
                          .append("，位于位置/描述：").append(placeDesc)
                          .append("，行为状态为：").append(behavior).append("\n");
        }

        // 拼装最终的 Context (包含偏差学习和行为轨迹)
        String finalContext = correctionContext.toString() + "历史足迹轨迹：\n" + contextBuilder.toString();

        // 4. 调用 AI 接口进行推演
        return aiProvider.reasonBehavior(finalContext);
    }

    /**
     * 保存用户对 AI 推理结果的确认/修正反馈
     */
    @Transactional
    public void saveFeedback(FeedbackRequest request) {
        PredictionFeedback feedback = new PredictionFeedback();
        feedback.setAnimalId(request.getAnimalId());
        feedback.setPredictedBehavior(request.getPredictedBehavior());
        feedback.setActualBehavior(request.getActualBehavior());
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setRecordedAt(LocalDateTime.now());
        
        predictionFeedbackRepository.save(feedback);
    }

    /**
     * 获取动物最新的生活叙事记录 (AI 成长日记)
     */
    public String getLatestNarrative(Long animalId) {
        List<AnimalLifeNarrative> narratives = animalLifeNarrativeRepository.findByAnimalIdOrderByStartTimeDesc(animalId);
        if (narratives != null && !narratives.isEmpty()) {
            return narratives.get(0).getNarrativeContent();
        }
        return "还没有生成该小动物的生活记录，添加足迹后，AI 行为分析师将自动为它写日记喔！🐾";
    }

    /**
     * 获取动物全部的生活叙事记录 (按时间升序)
     */
    public List<java.util.Map<String, Object>> getAllNarratives(Long animalId) {
        List<AnimalLifeNarrative> narratives = animalLifeNarrativeRepository.findByAnimalIdOrderByStartTimeDesc(animalId);
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
        if (narratives != null && !narratives.isEmpty()) {
            for (int i = narratives.size() - 1; i >= 0; i--) {
                AnimalLifeNarrative n = narratives.get(i);
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("content", n.getNarrativeContent());
                map.put("time", n.getEndTime() != null ? n.getEndTime().format(formatter) : "未知时间");
                list.add(map);
            }
        }
        return list;
    }
}
