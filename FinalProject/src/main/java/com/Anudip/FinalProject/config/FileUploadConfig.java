package com.Anudip.FinalProject.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    private String projectRoot;
    private String uploadsPath;

    @PostConstruct
    public void init() {
        // Get the project root directory
        projectRoot = System.getProperty("user.dir");
        uploadsPath = projectRoot + File.separator + "src" + File.separator + "main" + 
                      File.separator + "resources" + File.separator + "static" + File.separator + "uploads";
        
        // Also create a directory in target for serving during development
        String targetUploadsPath = projectRoot + File.separator + "target" + File.separator + 
                                   "classes" + File.separator + "static" + File.separator + "uploads";
        
        // Ensure directories exist
        createUploadsDirectory(uploadsPath);
        createUploadsDirectory(targetUploadsPath + File.separator + "book");
        createUploadsDirectory(targetUploadsPath + File.separator + "user");
        
        System.out.println("File upload directory initialized: " + uploadsPath);
    }
    
    private void createUploadsDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("Created upload directory: " + path);
            }
        }
    }

    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String projectRoot = System.getProperty("user.dir");
        String staticUploadsPath = projectRoot + File.separator + "src" + File.separator + "main" + 
                                   File.separator + "resources" + File.separator + "static" + File.separator + "uploads";
        
        String targetUploadsPath = "file:" + projectRoot + File.separator + "target" + File.separator + 
                                   "classes" + File.separator + "static" + File.separator + "uploads";
        
        // Add resource handlers for both directories
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(targetUploadsPath + "/")
                .addResourceLocations("file:" + staticUploadsPath + "/")
                .addResourceLocations("classpath:/static/uploads/");
        
        System.out.println("Serving uploads from: " + staticUploadsPath);
    }
}

