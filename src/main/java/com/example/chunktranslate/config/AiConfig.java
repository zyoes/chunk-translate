package com.example.chunktranslate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.translation")
public class AiConfig {

    /** 阿里云 AccessKey ID */
    private String accessKeyId;

    /** 阿里云 AccessKey Secret */
    private String accessKeySecret;

    /** API Endpoint */
    private String endpoint = "mt.cn-hangzhou.aliyuncs.com";

    /** 翻译场景：general（通用）、medical（医疗）等 */
    private String scene = "general";

    /** 文本格式：text 或 html */
    private String formatType = "text";
}
