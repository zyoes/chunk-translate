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

    /**
     * 根据状态码转换为枚举
     *
     * @param code 状态码
     * @return 对应的枚举值，找不到时返回 UPLOADED
     */
    public static DocumentStatus fromCode(int code) {
        for (DocumentStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return UPLOADED;
    }
}
