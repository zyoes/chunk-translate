package com.example.chunktranslate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云机器翻译 API 配置。
 * <p>从 application.yml 中 aliyun.translation 前缀读取配置项，
 * 用于初始化 {@link com.example.chunktranslate.service.translation.ALimtTranslationClient}。</p>
 */
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
