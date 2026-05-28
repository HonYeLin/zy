package com.pawtrack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private com.pawtrack.interceptor.AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /images/unclassified/** to physical file path on disk
        String userDir = System.getProperty("user.dir");
        File baseDir = new File(userDir);
        if (!userDir.endsWith("backend")) {
            baseDir = new File(baseDir, "backend");
        }
        
        File uploadDir = new File(baseDir, "uploads/images");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + uploadDir.getAbsolutePath() + "/");
    }
}
