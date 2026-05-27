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
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=" + apiKey;

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
        
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent?key=" + apiKey;

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
}
