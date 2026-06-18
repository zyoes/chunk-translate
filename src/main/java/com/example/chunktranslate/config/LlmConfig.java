package com.example.chunktranslate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek LLM 配置（AI 润色）
 * <p>
 * 读取 application.yml 中 deepseek.api 前缀的配置项。
 * DeepSeek API 兼容 OpenAI 格式，用 OkHttp 直接调用。
 * 用于第二级润色：对机器翻译的粗糙译文进行自然度优化。
 * </p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "deepseek.api")
public class LlmConfig {

    /** API 请求地址 */
    private String url = "https://api.deepseek.com/v1/chat/completions";

    /** API Key */
    private String key;

    /** 模型名称 */
    private String model = "deepseek-chat";

    /** 单次请求最大 Token 数（润色长译文需足够大，避免输出截断） */
    private Integer maxTokens = 16384;

    /** 温度参数（润色建议低温，保持确定性） */
    private Double temperature = 0.3;
}