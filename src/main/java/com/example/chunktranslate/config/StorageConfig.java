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
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
