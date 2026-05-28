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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class UploadController {

    @Value("${ai.dashscope.api-key}")
    private String dashscopeApiKey;

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
            file.transferTo(destFile);

            // Return relative URL that works across environments
            String fileUrl = "/images/unclassified/" + newFilename;
            response.put("url", fileUrl);

            // 3. Call DashScope Qwen-VL-Max to recognize the image
            byte[] fileBytes = Files.readAllBytes(destFile.toPath());
            String mimeType = file.getContentType();
            Map<String, Object> aiData = analyzeImage(fileBytes, mimeType);
            response.put("aiData", aiData);

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
                "text", "你是一个高校校园小动物识别助手。请识别图片中的小动物，并提取信息返回为一个 JSON 对象。必须严格遵循 JSON 格式，属性包括：\n" +
                        "1. 'type': 只能是 'Cat'（猫猫）、'Dog'（狗子）、'Other'（其他） 之一。\n" +
                        "2. 'nickname': 为它取一个简短可爱的名字（例如'大黄'、'咪咪'），如果没有明显特征可按颜色或状态取名。\n" +
                        "3. 'features': 仔细描述它的毛发颜色、身体花纹、五官特征、耳朵是否有缺角等细节特征，不要包含地点或动作描述，不超过60字。\n" +
                        "4. 'behaviorTag': 根据图片中它正在做的事，只能是 'EATING'、'SLEEPING'、'PLAYING'、'SUNBATHING'、'WALKING'、'OTHER' 之一。\n" +
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
}
