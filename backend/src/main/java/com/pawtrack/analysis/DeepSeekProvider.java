package com.pawtrack.analysis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class DeepSeekProvider implements IAIProvider {

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private String callDeepSeek(String prompt) {
        String url = "https://api.deepseek.com/chat/completions";

        Map<String, Object> requestBody = Map.of(
            "model", "deepseek-v4-pro",
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            ),
            "thinking", Map.of("type", "enabled"),
            "reasoning_effort", "high",
            "stream", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            return extractTextFromResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            return "推理失败：" + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null && message.containsKey("content")) {
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            System.err.println("解析 DeepSeek 响应失败");
        }
        return "无法解析推理结果";
    }

    @Override
    public String reasonBehavior(String context) {
        String prompt;
        if (context.startsWith("你是一个") || context.startsWith("你区域") || context.startsWith("你是一位")) {
            prompt = context;
        } else {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            String clickTime = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String dayOfWeek = now.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINA);

            prompt = "你是一位幽默而灵敏的校园流浪动物行为学分析专家。\n" +
                    "当前用户发起推测的系统时间为：" + clickTime + "（" + dayOfWeek + "）。\n\n" +
                    "基于以下该小动物的历史轨迹数据，分析小动物在以往类似时间段的行为规律，并推测它“现在”最可能在做什么：\n" +
                    context + "\n" +
                    "【推测任务要求】：\n" +
                    "1. 绝对不要输出任何推理过程、分析逻辑、前言、思考步骤、分点说明或 Markdown 标签（如 #, *, - 等）。\n" +
                    "2. 最终输出的文本必须**仅包含结论**，格式需要采用“最近行为总结 + 现在相同时间行为推测 + 微微的滑稽调侃”的流畅一句话口语段落。\n" +
                    "   - 参考范例：\"它最近这会这个时间都在晒太阳，去它喜欢躺的草坪找找看吧，真是只大懒猫。\"\n" +
                    "   - 参考范例：\"这个点历史记录里它通常已经在食堂门口蹲守小鱼干了，现在估计又在对路过的学生疯狂眨眼讨零食呢，真是个不折不扣的校园吃货。\"\n" +
                    "3. 请直接输出推测结论本身，控制在40-90字之间。";
        }

        return callDeepSeek(prompt);
    }

    @Override
    public String classifyBehavior(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "OTHER";
        }

        String prompt = "你是一个动物行为分类器。根据下面提供的小动物特征或当前状态描述，将其分类到以下6个预定义状态标签中的一个。只能返回这6个单词之一（EATING, SLEEPING, PLAYING, SUNBATHING, WALKING, OTHER），不要返回任何其他内容、标点符号或解释说明。\n\n描述内容：" + description;

        try {
            String result = callDeepSeek(prompt);
            if (result != null) {
                result = result.trim().toUpperCase();
                if (result.contains("EATING")) return "EATING";
                if (result.contains("SLEEPING")) return "SLEEPING";
                if (result.contains("PLAYING")) return "PLAYING";
                if (result.contains("SUNBATHING")) return "SUNBATHING";
                if (result.contains("WALKING")) return "WALKING";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "OTHER";
    }

    @Override
    public Long matchExistingAnimal(String type, String newFeatures, List<com.pawtrack.entity.Animal> existingAnimals) {
        if (existingAnimals == null || existingAnimals.isEmpty() || newFeatures == null || newFeatures.trim().isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("你是一个高校校园小动物特征匹配助手。请根据下面提供的新观察到的小动物特征，判断它是否已经是数据库中已有的某个小动物。\n\n");
        sb.append("新发现的小动物信息：\n");
        sb.append("- 种类: ").append(type).append("\n");
        sb.append("- 特征: ").append(newFeatures).append("\n\n");
        sb.append("数据库中已有的同类小动物列表：\n");
        for (com.pawtrack.entity.Animal animal : existingAnimals) {
            sb.append("- ID: ").append(animal.getId())
              .append(", 昵称: ").append(animal.getName())
              .append(", 特征描述: ").append(animal.getDescription() != null ? animal.getDescription() : "无特征描述")
              .append("\n");
        }
        sb.append("\n请对比他们的毛发颜色、条纹五官等身体特征：\n");
        sb.append("1. 如果有非常高的匹配度（外观特征基本一致），认为他们是同一个小动物，请【仅返回】其 ID（数字，例如 \"1\"）。\n");
        sb.append("2. 如果明显不吻合，或者找不到匹配的，或者有任何冲突（如毛发颜色完全不同），请【仅返回】 \"NEW\"。\n");
        sb.append("注意：只能返回数字 ID 或单词 \"NEW\"，不要返回任何其他内容、标点符号、markdown 或解释性文字。");

        try {
            String result = callDeepSeek(sb.toString());
            if (result != null) {
                result = result.trim();
                System.out.println("DeepSeek 实体匹配返回结果: " + result);
                if (result.equalsIgnoreCase("NEW")) {
                    return null;
                }
                // 移除非数字字符，防止大模型返回带标点
                result = result.replaceAll("[^0-9]", "");
                if (!result.isEmpty()) {
                    return Long.parseLong(result);
                }
            }
        } catch (Exception e) {
            System.err.println("DeepSeek 实体匹配失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String optimizeAnimalFeatures(String oldFeatures, String newFeatures) {
        if (oldFeatures == null || oldFeatures.trim().isEmpty()) {
            return newFeatures;
        }
        if (newFeatures == null || newFeatures.trim().isEmpty()) {
            return oldFeatures;
        }

        String prompt = "你是一个文本特征融合与整合专家。请将同一个校园小动物的历史特征描述与本次新发现的特征描述进行优化融合成一段话，消除重复与冗余，保留更丰富的细节。\n" +
                "- 已保存历史特征: " + oldFeatures + "\n" +
                "- 本次新识别特征: " + newFeatures + "\n\n" +
                "要求：\n" +
                "1. 将其融合成一段逻辑顺畅、核心特征突出的短文（包含颜色、花纹、耳朵、体型等外观关键点）。\n" +
                "2. 字数控制在100字以内。\n" +
                "3. 【只输出】优化融合后的纯文本内容，不要有任何前缀、后缀、代码块包装或说明。";

        try {
            String result = callDeepSeek(prompt);
            if (result != null && !result.trim().isEmpty()) {
                return result.trim();
            }
        } catch (Exception e) {
            System.err.println("DeepSeek 特征优化融合失败: " + e.getMessage());
            e.printStackTrace();
        }
        // 发生异常时，简单拼接作为兜底
        return oldFeatures + "；" + newFeatures;
    }

    @Override
    public String generateAnimalSummary(com.pawtrack.entity.Animal animal, List<com.pawtrack.entity.LocationLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位动物行为观察专家和幽默作家。请根据以下校园流浪小动物的基本信息和历史足迹记录，为它生成一段有趣的简介。\n\n");
        sb.append("基本信息：\n");
        sb.append("- 昵称: ").append(animal.getName() != null ? animal.getName() : "未知").append("\n");
        sb.append("- 特征描述: ").append(animal.getDescription() != null ? animal.getDescription() : "无特征描述").append("\n\n");

        sb.append("历史足迹记录（近期活动）：\n");
        if (logs == null || logs.isEmpty()) {
            sb.append("暂无足迹记录。\n");
        } else {
            int limit = Math.min(logs.size(), 20); // 最多参考20条记录
            for (int i = 0; i < limit; i++) {
                com.pawtrack.entity.LocationLog log = logs.get(i);
                sb.append("- 时间: ").append(log.getRecordedAt() != null ? log.getRecordedAt() : "未知")
                  .append(", 地点: ").append(log.getDescription() != null ? log.getDescription() : "未知")
                  .append(", 行为状态: ").append(log.getBehaviorLabel() != null ? log.getBehaviorLabel() : "未知")
                  .append("\n");
            }
        }

        sb.append("\n【要求】：\n");
        sb.append("1. 简介内容需包含：动物外观特征 + 行为习惯 + 常见出没地点 + 微微的幽默评价。\n");
        sb.append("2. 格式必须流畅生动，直接输出段落内容，**绝对不要**包含任何 Markdown 格式符号（如 #、* 等）。\n");
        sb.append("3. 参考范例：“TA是一只全是黄色的小猫，喜欢到处乱逛，没事就晒晒太阳，时常在草地上出没，真是只悠闲的咪。”\n");
        sb.append("4. 字数控制在100字以内。");

        try {
            String result = callDeepSeek(sb.toString());
            if (result != null && !result.trim().isEmpty()) {
                return result.trim().replaceAll("[#*]", "");
            }
        } catch (Exception e) {
            System.err.println("DeepSeek 动物简介生成失败: " + e.getMessage());
            e.printStackTrace();
        }
        return "一只神秘的小动物，还没有人足够了解它呢。";
    }
}
