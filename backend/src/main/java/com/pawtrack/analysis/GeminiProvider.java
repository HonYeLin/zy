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
public class GeminiProvider implements IAIProvider {

    @Value("${ai.gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String reasonBehavior(String context) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey;

        // 构建 Prompt
        String prompt = "你是一位动物行为学专家。基于以下轨迹序列，推测该动物在当前时间最可能在做什么？请简要说明逻辑并给出一个状态标签（如：睡觉、吃饭、玩耍、巡视领地等）。\n\n历史轨迹序列：\n" + context;

        // 构建请求体 (符合 Gemini API 的 JSON 结构)
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

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
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            System.err.println("解析 Gemini 响应失败");
        }
        return "无法解析推理结果";
    }

    @Override
    public String classifyBehavior(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "OTHER";
        }
        
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey;

        String prompt = "你是一个动物行为分类器。根据下面提供的小动物特征或当前状态描述，将其分类到以下6个预定义状态标签中的一个。只能返回这6个单词之一（EATING, SLEEPING, PLAYING, SUNBATHING, WALKING, OTHER），不要返回任何其他内容、标点符号或解释说明。\\n\\n描述内容：" + description;

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            String result = extractTextFromResponse(response);
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

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey;

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
        sb.append("1. 如果有非常高的匹配度（外观特征基本一致），认为他们是同小动物，请【仅返回】其 ID（数字，例如 \"1\"）。\n");
        sb.append("2. 如果明显不吻合，或者找不到匹配的，或者有任何冲突（如毛发颜色完全不同），请【仅返回】 \"NEW\"。\n");
        sb.append("注意：只能返回数字 ID 或单词 \"NEW\"，不要返回任何其他内容、标点符号、markdown 或解释性文字。");

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", sb.toString())
                ))
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            String result = extractTextFromResponse(response);
            if (result != null) {
                result = result.trim();
                System.out.println("Gemini 实体匹配返回结果: " + result);
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
            System.err.println("Gemini 实体匹配失败: " + e.getMessage());
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

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + apiKey;

        String prompt = "你是一个文本特征融合与整合专家。请将同一个校园小动物的历史特征描述与本次新发现的特征描述进行优化融合成一段话，消除重复与冗余，保留更丰富的细节。\n" +
                "- 已保存历史特征: " + oldFeatures + "\n" +
                "- 本次新识别特征: " + newFeatures + "\n\n" +
                "要求：\n" +
                "1. 将其融合成一段逻辑顺畅、核心特征突出的短文（包含颜色、花纹、耳朵、体型等外观关键点）。\n" +
                "2. 字数控制在100字以内。\n" +
                "3. 【只输出】优化融合后的纯文本内容，不要有任何前缀、后缀、代码块包装或说明。";

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            String result = extractTextFromResponse(response);
            if (result != null && !result.trim().isEmpty()) {
                return result.trim();
            }
        } catch (Exception e) {
            System.err.println("Gemini 特征优化融合失败: " + e.getMessage());
            e.printStackTrace();
        }
        // 发生异常时，简单拼接作为兜底
        return oldFeatures + "；" + newFeatures;
    }
}
