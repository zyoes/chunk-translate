package com.example.chunktranslate.common.enums;

import lombok.Getter;

@Getter
public enum DocumentStatus {
    UPLOADED(0, "已上传"),
    PARSING(1, "解析中"),
    PARSED(2, "已解析"),
    PARSE_FAILED(3, "解析失败");

    private final int code;
    private final String desc;

    DocumentStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
