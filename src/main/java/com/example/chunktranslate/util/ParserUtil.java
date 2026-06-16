package com.example.chunktranslate.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文档解析公共工具类
 * <p>
 * 提供所有解析器共用的工具方法：
 * <ul>
 *   <li>{@link #truncate(String, int)} — 截断字符串用于摘要展示</li>
 *   <li>{@link #estimateTokens(String)} — 粗略估算文本的 Token 数（用于判断是否超出 AI 模型限制）</li>
 * </ul>
 * </p>
 *
 * <p>Token 估算规则：</p>
 * <ul>
 *   <li>含中文字符的 token：按字符数计算（每个汉字约 1 token）</li>
 *   <li>纯英文 token：按空格分词后的词数计算（每个词约 1~2 tokens）</li>
 * </ul>
 */
@Slf4j
@Component
public class ParserUtil {

    /**
     * 截断字符串
     *
     * @param text   字符串
     * @param maxLen 最大长度
     * @return 截断后的字符串
     */
    public String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }

    /**
     * 估计字符串的 token 数
     *
     * @param text 字符串
     * @return token 数
     */
    public int estimateTokens(String text) {
        int chineseChars = 0;
        int englishWords = 0;
        for (String token : text.split("\\s+")) {
            if (token.matches(".*[\\u4e00-\\u9fa5].*")) {
                chineseChars += token.length();
            } else {
                englishWords++;
            }
        }
        return chineseChars + englishWords;
    }
}
