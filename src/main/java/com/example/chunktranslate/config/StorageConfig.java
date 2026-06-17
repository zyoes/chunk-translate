package com.example.chunktranslate.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Data
@Configuration
@ConfigurationProperties(prefix = "storage.local")
public class StorageConfig {

    private String basePath = "./uploads";

    @PostConstruct
    public void init() {
        File dir = new File(basePath);
        // 将相对路径转为绝对路径，避免 transferTo() 解析到 Tomcat 临时目录
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), basePath);
        }
        basePath = dir.getAbsolutePath();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
