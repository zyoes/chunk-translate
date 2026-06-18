package com.example.chunktranslate.service.translation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.config.LlmConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekPolishClient {

    private final LlmConfig llmConfig;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final String POLISH_PROMPT_TEMPLATE = """
            你是一名专业技术翻译审校人员。以下是一段机器翻译的结果，请对其进行润色，使其更加自然流畅。
            要求：
            1. 修正生硬的机器翻译痕迹
            2. 保持专业术语准确
            3. 保留原文格式（标题、列表、代码块、表格等）
            4. 只输出润色后的译文，不要任何解释或额外内容
            
            原文（%s）：
            %s
            
            机器翻译结果（%s）：
            %s
            """;

    /**
     * 使用DeepSeek API进行润色
     *
     * @param sourceText         原文
     * @param machineTranslation 机器翻译结果
     * @param sourceLang         原文语言
     * @param targetLang         目标语言
     * @return 润色后的译文
     */
    public String polish(String sourceText, String machineTranslation, String sourceLang, String targetLang) {
        // 构建用户提示
        String userPrompt = POLISH_PROMPT_TEMPLATE.format(sourceLang, sourceText, targetLang, machineTranslation);

        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", llmConfig.getModel());
        requestBody.put("temperature", llmConfig.getTemperature());
        requestBody.put("max_tokens", llmConfig.getMaxTokens());

        // 构建消息数组
        JSONArray message = new JSONArray();
        message.add(new JSONObject().fluentPut("role", "user").fluentPut("content", userPrompt));
        requestBody.put("messages", message);

        // 构建请求
        Request request = new Request.Builder()
                .url(llmConfig.getUrl())
                .addHeader("Authorization", "Bearer " + llmConfig.getKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toString(), JSON_MEDIA_TYPE))
                .build();

        // 发送请求并处理响应
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("DeepSeek API 调用失败: code={}, body={}", response.code(), response.body());
                throw new BusinessException(ResultCode.TRANSLATION_FAIL);
            }

            // 解析响应
            String responseJson = response.body().string();
            JSONObject json = JSON.parseObject(responseJson);

            // 获取润色后的译文
            String polished = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            // 记录润色结果
            log.debug("AI润色完成: 原文长度={}, 译文长度={}, 润色后长度={}",
                    sourceText.length(), machineTranslation.length(), polished.length());

            return polished.trim();

        } catch (IOException e) {
            log.error("DeepSeek API 调用异常", e);
            throw new BusinessException(ResultCode.TRANSLATION_FAIL);
        }
    }
}
