package com.pawtrack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class UploadController {

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();
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
            
            // 1. Target directory: src/main/resources/static/images/unclassified
            File staticDir = new File(baseDir, "src/main/resources/static/images/unclassified");
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

            // 2. Also save to target/classes/static/images/unclassified for instant classloader fallback
            File targetDir = new File(baseDir, "target/classes/static/images/unclassified");
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            Files.copy(destFile.toPath(), new File(targetDir, newFilename).toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Return URL
            String fileUrl = "http://localhost:8080/images/unclassified/" + newFilename;
            response.put("url", fileUrl);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            response.put("error", "文件保存失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
