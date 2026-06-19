package com.example.chunktranslate.common.enums;

import lombok.Getter;

@Getter
public enum ChunkStatus {
    PENDING(0, "待翻译"),
    TRANSLATING(1, "翻译中"),
    COMPLETED(2, "已完成"),
    FAILED(3, "翻译失败");

    private final int code;
    private final String desc;

    ChunkStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     *
     * @param code 状态码
     * @return 对应的枚举值，找不到时返回 PENDING
     */
    public static ChunkStatus fromCode(int code) {
        for (ChunkStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return PENDING;
    }
}
