package com.pawtrack.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/images")
@CrossOrigin(origins = "*")
public class AdminImageController {

    private File getUploadDir() {
        String userDir = System.getProperty("user.dir");
        File baseDir = new File(userDir);
        if (!userDir.endsWith("backend")) {
            baseDir = new File(baseDir, "backend");
        }
        return new File(baseDir, "uploads/images");
    }

    @GetMapping
    public ResponseEntity<List<Map<String, String>>> listImages() {
        File uploadDir = getUploadDir();
        List<Map<String, String>> images = new ArrayList<>();
        if (uploadDir.exists() && uploadDir.isDirectory()) {
            collectImagesRecursively(uploadDir, uploadDir, images);
        }
        return ResponseEntity.ok(images);
    }

    private void collectImagesRecursively(File rootDir, File currentDir, List<Map<String, String>> images) {
        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectImagesRecursively(rootDir, file, images);
                } else if (file.isFile()) {
                    String rootPath = rootDir.getAbsolutePath();
                    String filePath = file.getAbsolutePath();
                    String relativePath = filePath.substring(rootPath.length()).replace('\\', '/');
                    if (relativePath.startsWith("/")) {
                        relativePath = relativePath.substring(1);
                    }
                    
                    Map<String, String> imgInfo = new HashMap<>();
                    imgInfo.put("filename", relativePath);
                    imgInfo.put("url", "/images/" + relativePath);
                    imgInfo.put("size", String.valueOf(file.length()));
                    images.add(imgInfo);
                }
            }
        }
    }

    @DeleteMapping("/{*filename}")
    public ResponseEntity<?> deleteImage(@PathVariable String filename) {
        try {
            if (filename == null || filename.isEmpty()) {
                return ResponseEntity.badRequest().body("文件名不能为空");
            }
            if (filename.startsWith("/")) {
                filename = filename.substring(1);
            }
            File uploadDir = getUploadDir();
            File targetFile = new File(uploadDir, filename);
            if (targetFile.exists() && targetFile.isFile()) {
                Files.delete(targetFile.toPath());
                
                // 级联清理空的父文件夹，直到上传根目录
                File parent = targetFile.getParentFile();
                while (parent != null && !parent.equals(uploadDir)) {
                    if ("unclassified".equals(parent.getName())) {
                        break; // 保持 unclassified 文件夹不被删除
                    }
                    File[] children = parent.listFiles();
                    if (children == null || children.length == 0) {
                        Files.delete(parent.toPath());
                        parent = parent.getParentFile();
                    } else {
                        break;
                    }
                }
                
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
