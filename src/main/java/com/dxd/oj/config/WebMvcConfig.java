package com.dxd.oj.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${oj.file.upload-dir:${user.home}/online-judge/uploads}")
    private String uploadDir;

    @Value("${oj.file.public-url-prefix:/uploads}")
    private String publicUrlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String pattern = normalizeUrlPrefix(publicUrlPrefix) + "/**";
        Path dirPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        registry.addResourceHandler(pattern)
                .addResourceLocations("file:" + dirPath + "/");
    }

    private String normalizeUrlPrefix(String prefix) {
        String result = prefix == null ? "/uploads" : prefix.trim();
        if (!result.startsWith("/")) {
            result = "/" + result;
        }
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
