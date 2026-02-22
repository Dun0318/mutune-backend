package com.mutune.backend.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 프론트-백엔드 CORS 허용 설정 + uploads 정적 리소스 매핑
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 API 경로 허용
                        .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173") // 프론트 주소
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowCredentials(true);
            }


            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {

                String uploadPath = Paths.get(System.getProperty("user.dir"), "uploads")
                        .toAbsolutePath()
                        .toUri()
                        .toString();

                if (!uploadPath.startsWith("file:")) {
                    uploadPath = "file:" + uploadPath;
                }

                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations(uploadPath);
            }
        };
    }
}
