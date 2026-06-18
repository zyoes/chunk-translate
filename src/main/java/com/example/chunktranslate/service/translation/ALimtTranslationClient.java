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

import java.util.ArrayList;
import java.util.List;

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

    /** 阿里云翻译 API 单次最大字符数 */
    private static final int MAX_CHARS_PER_REQUEST = 5000;

    public String translate(String text, String sourceLanguage, String targetLanguage) {
        // 超长文本分段翻译（阿里云 API 限制 10000 字符，用 5000 留安全余量）
        if (text.length() > MAX_CHARS_PER_REQUEST) {
            return translateLongText(text, sourceLanguage, targetLanguage);
        }
        return doTranslate(text, sourceLanguage, targetLanguage);
    }

    /**
     * 分段翻译超长文本
     * <p>按句号/换行符拆分，每段不超过 {@value #MAX_CHARS_PER_REQUEST} 字符</p>
     */
    private String translateLongText(String text, String sourceLanguage, String targetLanguage) {
        log.info("文本超长({}字符), 启用分段翻译", text.length());
        List<String> segments = splitText(text, MAX_CHARS_PER_REQUEST);
        StringBuilder result = new StringBuilder();
        for (String segment : segments) {
            result.append(doTranslate(segment, sourceLanguage, targetLanguage));
        }
        return result.toString();
    }

    /**
     * 按换行符或句号拆分文本，每段不超过 maxLen 字符
     */
    private List<String> splitText(String text, int maxLen) {
        List<String> segments = new ArrayList<>();
        // 优先按换行拆分
        String[] parts = text.split("(?<=\n)");
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            if (current.length() + part.length() > maxLen && !current.isEmpty()) {
                segments.add(current.toString());
                current = new StringBuilder();
            }
            // 单个 part 本身就超过 maxLen，强制截断
            if (part.length() > maxLen) {
                if (!current.isEmpty()) {
                    segments.add(current.toString());
                    current = new StringBuilder();
                }
                for (int i = 0; i < part.length(); i += maxLen) {
                    segments.add(part.substring(i, Math.min(i + maxLen, part.length())));
                }
            } else {
                current.append(part);
            }
        }
        if (!current.isEmpty()) {
            segments.add(current.toString());
        }
        return segments;
    }

    /**
     * 执行单次阿里云翻译 API 调用
     */
    private String doTranslate(String text, String sourceLanguage, String targetLanguage) {
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
            log.error("阿里云翻译返回空数据: sourceLang={}, targetLang={}, code={}, message={}",
                    sourceLanguage, targetLanguage,
                    response.getBody() != null ? response.getBody().getCode() : "null",
                    response.getBody() != null ? response.getBody().getMessage() : "null");
            throw new BusinessException(ResultCode.TRANSLATION_FAIL);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("阿里云机器翻译失败: sourceLang={}, targetLang={}", sourceLanguage, targetLanguage, e);
            throw new BusinessException(ResultCode.TRANSLATION_FAIL);
        }
    }
}
