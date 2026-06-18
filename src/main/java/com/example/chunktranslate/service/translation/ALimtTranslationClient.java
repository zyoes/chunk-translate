package com.example.chunktranslate.service.translation;

import com.aliyun.alimt20181012.Client;
import com.aliyun.alimt20181012.models.TranslateGeneralRequest;
import com.aliyun.alimt20181012.models.TranslateGeneralResponse;
import com.aliyun.teaopenapi.models.Config;
import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.config.AiConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ALimtTranslationClient {

    private final AiConfig aiConfig;

    private Client client;

    @PostConstruct
    public void init() {
        try {
            // 配置阿里云机器翻译客户端参数
            Config config = new Config()
                    .setAccessKeyId(aiConfig.getAccessKeyId())
                    .setAccessKeySecret(aiConfig.getAccessKeySecret());

            // 设置服务端地址
            config.endpoint = aiConfig.getEndpoint();
            this.client = new Client(config);
            log.info("阿里云机器翻译客户端初始化成功, endpoint={}", aiConfig.getEndpoint());
        } catch (Exception e) {
            log.error("阿里云机器翻译客户端初始化失败", e);
            throw new RuntimeException("阿里云翻译客户端初始化失败", e);
        }
    }

    public String translate(String text, String sourceLanguage, String targetLanguage) {
        try {
            // 创建翻译请求
            TranslateGeneralRequest request = new TranslateGeneralRequest()
                    .setFormatType(aiConfig.getFormatType())
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(targetLanguage)
                    .setSourceText(text)
                    .setScene(aiConfig.getScene());

            // 发送翻译请求并获取响应
            TranslateGeneralResponse response = client.translateGeneral(request);

            // 处理响应数据
            if (response.getBody() != null && response.getBody().getData() != null) {
                String translated = response.getBody().getData().getTranslated();
                log.debug("机器翻译完成: sourceLang={}, targetLang={}, 原文长度={}, 译文长度={}",
                        sourceLanguage, targetLanguage, text.length(), translated.length());
                return translated;
            }
            throw new BusinessException(ResultCode.TRANSLATION_FAIL);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("阿里云机器翻译失败: sourceLang={}, targetLang={}", sourceLanguage, targetLanguage, e);
            throw new BusinessException(ResultCode.TRANSLATION_FAIL);
        }
    }
}
