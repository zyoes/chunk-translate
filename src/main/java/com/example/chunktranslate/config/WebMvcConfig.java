package com.example.chunktranslate.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Spring MVC 全局配置。
 * <ul>
 *   <li>CORS：允许所有来源访问 /api/**，支持跨域携带 Cookie</li>
 *   <li>静态资源：将上传目录映射为 /uploads/** URL 路径</li>
 * </ul>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StorageConfig storageConfig;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = new File(storageConfig.getBasePath()).getAbsolutePath()
                .replace("\\", "/");
        if (!absolutePath.endsWith("/")) {
            absolutePath += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + absolutePath);
    }
}
