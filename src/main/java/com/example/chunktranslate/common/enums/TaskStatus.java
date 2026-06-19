package com.example.chunktranslate.common.enums;

import lombok.Getter;

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

    public static TaskStatus fromCode(int code) {
        for (TaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return IN_PROGRESS;
    }
}
