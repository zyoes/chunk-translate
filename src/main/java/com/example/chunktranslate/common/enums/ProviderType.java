package com.example.chunktranslate.common.enums;

import lombok.Getter;

@Getter
public enum ProviderType {
    LOCAL("local"),
    GITHUB("github");

    private final String value;

    ProviderType(String value) {
        this.value = value;
    }
}
