package com.example.chunktranslate.common.enums;

import lombok.Getter;

/**
 * 用户角色枚举。
 * <p>用户角色分为管理员（admin）和普通用户（user），存储在 user 表的 role 字段。</p>
 */
@Getter
public enum UserRole {
    ADMIN("admin"),
    USER("user");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }
}
