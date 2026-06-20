package com.example.chunktranslate.common.enums;

import lombok.Getter;

/**
 * 第三方登录提供商类型枚举。
 * <p>目前支持表单登录（local）和 GitHub OAuth2 登录两种方式。</p>
 */
@Getter
public enum ProviderType {
    LOCAL("local"),
    GITHUB("github");

    private final String value;

    ProviderType(String value) {
        this.value = value;
    }
}
