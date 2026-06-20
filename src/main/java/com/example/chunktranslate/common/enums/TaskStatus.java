package com.example.chunktranslate.common.enums;

import lombok.Getter;

/**
 * 翻译任务状态枚举。
 * <p>对应 translation_task 表中整个翻译任务的状态，与单个分块的 {@link TranslationStatus} 不同，
 * 任务状态表示批次翻译的聚合结果。</p>
 */
@Getter
public enum TaskStatus {
    IN_PROGRESS(0, "进行中"),
    COMPLETED(1, "已完成"),
    FAILED(2, "失败");

    private final int code;
    private final String desc;

    TaskStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据状态码获取枚举。
     *
     * @param code 状态码
     * @return 对应的枚举值，找不到时返回 IN_PROGRESS
     */
    public static TaskStatus fromCode(int code) {
        for (TaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return IN_PROGRESS;
    }
}
