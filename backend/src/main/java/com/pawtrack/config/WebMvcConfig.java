package com.pawtrack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /images/unclassified/** to physical file path on disk
        String userDir = System.getProperty("user.dir");
        File baseDir = new File(userDir);
        if (!userDir.endsWith("backend")) {
            baseDir = new File(baseDir, "backend");
        }
        
        File uploadDir = new File(baseDir, "src/main/resources/static/images");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadDir.getAbsolutePath() + "/");
    }
}
