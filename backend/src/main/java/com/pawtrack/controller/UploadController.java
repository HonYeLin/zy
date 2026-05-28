package com.pawtrack.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawtrack.entity.Animal;
import com.pawtrack.repository.AnimalRepository;
import com.pawtrack.analysis.IAIProvider;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class UploadController {

    @Value("${ai.dashscope.api-key}")
    private String dashscopeApiKey;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private IAIProvider aiProvider;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        if (file.isEmpty()) {
            response.put("error", "文件不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Determine user.dir
            String userDir = System.getProperty("user.dir");
            File baseDir = new File(userDir);
            if (!userDir.endsWith("backend")) {
                baseDir = new File(baseDir, "backend");
            }
            
            // 1. Target directory: external uploads/images/unclassified
            File staticDir = new File(baseDir, "uploads/images/unclassified");
            if (!staticDir.exists()) {
                staticDir.mkdirs();
            }

            // 2. 提取字节流传给 AI 识别
            byte[] fileBytes = file.getBytes();
            String mimeType = file.getContentType();
            Map<String, Object> aiData = analyzeImage(fileBytes, mimeType);
            System.out.println("AI Image Analysis Result: " + aiData);

            boolean hasAnimal = true; // 默认
            if (aiData == null) {
                hasAnimal = false;
            } else if (aiData.containsKey("hasAnimal")) {
                Object val = aiData.get("hasAnimal");
                if (val instanceof Boolean) {
                    hasAnimal = (Boolean) val;
                } else if (val instanceof String) {
                    hasAnimal = Boolean.parseBoolean((String) val);
                }
            }

            // 如果 AI 判断不存在小动物，则拒绝保存并返回错误
            if (!hasAnimal) {
                response.put("error", "未检测到小动物，请重新上传！");
                response.put("code", "NO_ANIMAL");
                return ResponseEntity.badRequest().body(response);
            }

            // 2.5 实体视觉去重比对 (Funnel Matching)
            boolean isExisting = false;
            String matchedNickname = null;
            Long matchedId = null;

            String type = (String) aiData.get("type");
            String features = (String) aiData.get("features");

            if (type != null && !type.trim().isEmpty() && features != null && !features.trim().isEmpty()) {
                List<Animal> candidates = animalRepository.findByBreed(type.trim());
                List<Animal> candidatesWithAvatar = new java.util.ArrayList<>();
                for (Animal animal : candidates) {
                    if (animal.getAvatarUrl() != null && !animal.getAvatarUrl().trim().isEmpty()) {
                        candidatesWithAvatar.add(animal);
                    }
                }

                if (!candidatesWithAvatar.isEmpty()) {
                    List<Animal> finalCandidates = new java.util.ArrayList<>();
                    if (candidatesWithAvatar.size() > 4) {
                        // 使用 DeepSeek 预筛选
                        List<Long> filteredIds = aiProvider.filterCandidateAnimals(features, candidatesWithAvatar);
                        System.out.println("DeepSeek 预筛选出的候选 ID: " + filteredIds);
                        for (Long id : filteredIds) {
                            for (Animal animal : candidatesWithAvatar) {
                                if (animal.getId().equals(id)) {
                                    finalCandidates.add(animal);
                                    break;
                                }
                            }
                        }
                        // 确保最多 4 个
                        if (finalCandidates.size() > 4) {
                            finalCandidates = finalCandidates.subList(0, 4);
                        }
                    } else {
                        finalCandidates = candidatesWithAvatar;
                    }

                    // 调用视觉模型进行终审
                    if (!finalCandidates.isEmpty()) {
                        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
                        if (mimeType == null) {
                            mimeType = "image/jpeg";
                        }
                        String dataUri = "data:" + mimeType + ";base64," + base64Image;

                        Map<String, Object> visualMatchResult = matchVisually(dataUri, finalCandidates);
                        System.out.println("视觉对比匹配结果: " + visualMatchResult);
                        
                        if (visualMatchResult != null) {
                            Object isExistVal = visualMatchResult.get("isExisting");
                            boolean visualIsExisting = false;
                            if (isExistVal instanceof Boolean) {
                                visualIsExisting = (Boolean) isExistVal;
                            } else if (isExistVal instanceof String) {
                                visualIsExisting = Boolean.parseBoolean((String) isExistVal);
                            }

                            if (visualIsExisting) {
                                Object matchedIdVal = visualMatchResult.get("matchedId");
                                if (matchedIdVal != null) {
                                    Long id = null;
                                    if (matchedIdVal instanceof Number) {
                                        id = ((Number) matchedIdVal).longValue();
                                    } else {
                                        try {
                                            id = Long.parseLong(matchedIdVal.toString().trim());
                                        } catch (Exception ignored) {}
                                    }
                                    
                                    if (id != null) {
                                        final Long finalId = id;
                                        java.util.Optional<Animal> matchedAnimal = animalRepository.findById(finalId);
                                        if (matchedAnimal.isPresent()) {
                                            isExisting = true;
                                            matchedId = finalId;
                                            matchedNickname = matchedAnimal.get().getName();
                                            System.out.println("====== 视觉匹配成功！已有小动物: ID=" + matchedId + ", 昵称=" + matchedNickname + " ======");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Map<String, Object> aiDataMutable = new HashMap<>(aiData);
            aiDataMutable.put("isExisting", isExisting);
            if (isExisting && matchedNickname != null) {
                aiDataMutable.put("nickname", matchedNickname);
                aiDataMutable.put("matchedId", matchedId);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                ext = ".jpg";
            }
            String newFilename = UUID.randomUUID().toString() + ext;

            // Save to staticDir
            File destFile = new File(staticDir, newFilename);
            Files.write(destFile.toPath(), fileBytes);

            // Return relative URL that works across environments
            String fileUrl = "/images/unclassified/" + newFilename;
            response.put("url", fileUrl);
            response.put("aiData", aiDataMutable);

            return ResponseEntity.ok(response);


        } catch (IOException e) {
            e.printStackTrace();
            response.put("error", "文件保存失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> analyzeImage(byte[] fileBytes, String mimeType) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(fileBytes);
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }
            String dataUri = "data:" + mimeType + ";base64," + base64Image;

            String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

            Map<String, Object> textPart = Map.of(
                "type", "text",
                "text", "你是一个高校校园流浪猫狗等小动物识别助手。\n" +
                        "【特别警告】：必须首先判断图片中是否包含真实的动物实体。如果图片是一盆植物、风景、纯文字截图或者其他非动物物体，请严格只返回 {\"hasAnimal\": false} 即可，绝不允许脑补动物特征！\n" +
                        "必须严格遵循 JSON 格式，属性包括：\n" +
                        "1. 'hasAnimal': 布尔值，判断图片中是否包含小动物（如猫、狗等）。如果不包含，只返回 {\"hasAnimal\": false} 即可。如果包含真实动物，则 'hasAnimal': true，并继续提供以下属性：\n" +
                        "2. 'type': 只能是 'Cat'（猫猫）、'Dog'（狗子）、'Other'（其他） 之一。\n" +
                        "3. 'nickname': 为它取一个简短可爱的名字（例如'大黄'、'咪咪'），如果没有明显特征可按颜色或状态取名。\n" +
                        "4. 'features': 仔细描述它的毛发颜色、身体花纹、五官特征、耳朵是否有缺角等细节特征，不要包含地点或动作描述，不超过60字。\n" +
                        "5. 'behaviorTag': 根据图片中它正在做的事，只能是 'EATING'、'SLEEPING'、'PLAYING'、'SUNBATHING'、'WALKING'、'OTHER' 之一。\n" +
                        "6. 'sceneDescription': 记录当前行为（对图片中整体画面进行描述，必须包含场景+主体动物+正在进行的动物行为，字数限制在30到50个字之间）。\n" +
                        "请只返回 JSON 对象本身，不要使用 ```json 或 ``` 包裹，不要包含任何前导或后缀文本。"
            );

            Map<String, Object> imagePart = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUri)
            );

            Map<String, Object> message = Map.of(
                "role", "user",
                "content", List.of(textPart, imagePart)
            );

            Map<String, Object> requestBody = Map.of(
                "model", "qwen-vl-max",
                "messages", List.of(message),
                "response_format", Map.of("type", "json_object")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + dashscopeApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    if (msg != null && msg.containsKey("content")) {
                        String content = (String) msg.get("content");
                        if (content != null) {
                            content = content.trim();
                            // Remove markdown code block if model ignored the instruction
                            if (content.startsWith("```")) {
                                int firstLineBreak = content.indexOf("\n");
                                int lastBackticks = content.lastIndexOf("```");
                                if (firstLineBreak != -1 && lastBackticks != -1 && lastBackticks > firstLineBreak) {
                                    content = content.substring(firstLineBreak + 1, lastBackticks).trim();
                                }
                            }
                            ObjectMapper mapper = new ObjectMapper();
                            return mapper.readValue(content, Map.class);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("DashScope 识图失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private File getFileFromUrl(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith("/images/")) {
            return null;
        }
        String userDir = System.getProperty("user.dir");
        File baseDir = new File(userDir);
        if (!userDir.endsWith("backend")) {
            baseDir = new File(baseDir, "backend");
        }
        File uploadDir = new File(baseDir, "uploads/images");
        String relativePath = avatarUrl.substring("/images/".length());
        return new File(uploadDir, relativePath);
    }

    private String getBase64ImageFromFile(File file) {
        try {
            if (file == null || !file.exists()) {
                return null;
            }
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(fileBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> matchVisually(String dataUri, List<Animal> finalCandidates) {
        try {
            String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

            List<Map<String, Object>> contentParts = new ArrayList<>();
            
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("你是一个高校校园小动物实体视觉比对助手。\n")
                         .append("图0是用户刚刚拍摄并上传的某只小动物的现场实拍图。\n")
                         .append("后续的图1、图2等（最多4张，如果有的话）是数据库中已有的候选小动物头像照片。\n\n")
                         .append("候选小动物对应关系如下：\n");
            
            for (int i = 0; i < finalCandidates.size(); i++) {
                Animal candidate = finalCandidates.get(i);
                promptBuilder.append("图").append(i + 1).append(" 对应小动物：ID=").append(candidate.getId())
                             .append(", 昵称=").append(candidate.getName()).append("\n");
            }
            
            promptBuilder.append("\n请仔细比对图0与后续所有候选头像中小动物的体貌特征（如毛色、条纹、斑块、耳朵、五官等）。\n")
                         .append("如果图0中的动物与某张候选头像的动物属于同一个实体（同一只猫/狗），请返回其 ID；\n")
                         .append("如果不属于其中任何一只，或者没有候选头像，请判定为新发现的动物，返回 \"NEW\"。\n\n")
                         .append("【输出要求】：\n")
                         .append("必须严格返回 JSON 格式，格式如下：\n")
                         .append("{\n")
                         .append("  \"matchedId\": 匹配的ID（若不匹配则为 null）,\n")
                         .append("  \"isExisting\": 匹配成功为 true，否则为 false,\n")
                         .append("  \"reason\": \"简要说明匹配/不匹配的比对理由，不超过30字\"\n")
                         .append("}\n")
                         .append("只返回 JSON 对象，不要包含任何前置/后置文字，不要用 ```json 包裹。");
            
            contentParts.add(Map.of(
                "type", "text",
                "text", promptBuilder.toString()
            ));
            
            // 图0
            contentParts.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUri)
            ));
            
            // 候选图
            for (Animal candidate : finalCandidates) {
                File candidateFile = getFileFromUrl(candidate.getAvatarUrl());
                String candidateBase64 = getBase64ImageFromFile(candidateFile);
                if (candidateBase64 != null) {
                    contentParts.add(Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", candidateBase64)
                    ));
                }
            }

            Map<String, Object> message = Map.of(
                "role", "user",
                "content", contentParts
            );

            Map<String, Object> requestBody = Map.of(
                "model", "qwen-vl-max",
                "messages", List.of(message),
                "response_format", Map.of("type", "json_object")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + dashscopeApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    if (msg != null && msg.containsKey("content")) {
                        String content = (String) msg.get("content");
                        if (content != null) {
                            content = content.trim();
                            if (content.startsWith("```")) {
                                int firstLineBreak = content.indexOf("\n");
                                int lastBackticks = content.lastIndexOf("```");
                                if (firstLineBreak != -1 && lastBackticks != -1 && lastBackticks > firstLineBreak) {
                                    content = content.substring(firstLineBreak + 1, lastBackticks).trim();
                                }
                            }
                            ObjectMapper mapper = new ObjectMapper();
                            return mapper.readValue(content, Map.class);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("视觉对比匹配失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}

