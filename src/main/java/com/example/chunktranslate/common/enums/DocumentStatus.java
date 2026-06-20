package com.example.chunktranslate.common.enums;

import lombok.Getter;

/**
 * 文档处理状态枚举。
 * <p>文档从上传到解析完成的状态流转：已上传→解析中→已解析/解析失败→翻译中，
 * 对应数据库 document 表的 status 字段。</p>
 */
@Getter
public enum DocumentStatus {
    UPLOADED(0, "已上传"),
    PARSING(1, "解析中"),
    PARSED(2, "已解析"),
    PARSE_FAILED(3, "解析失败"),
    TRANSLATING(4, "翻译中");

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
