package com.example.chunktranslate.common.enums;

import lombok.Getter;

/**
 * 翻译结果状态枚举。
 * <p>对应 translation_result 表中单条翻译记录的状态，表示某个分块在特定翻译阶段
 * （如阿里云机翻或 DeepSeek 润色）的处理结果。</p>
 */
@Getter
public enum TranslationStatus {
    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    COMPLETED(2, "已完成"),
    FAILED(3, "失败");

    private final int code;
    private final String desc;

    TranslationStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 对应的枚举值，找不到时返回 PENDING
     */
    public static TranslationStatus fromCode(int code) {
        for (TranslationStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return PENDING;
    }
}
