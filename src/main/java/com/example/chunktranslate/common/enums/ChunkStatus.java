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
}
