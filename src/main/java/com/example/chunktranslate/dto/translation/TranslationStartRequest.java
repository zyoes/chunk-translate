package com.example.chunktranslate.dto.translation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启动翻译请求 DTO
 */
@Data
@Schema(description = "翻译启动请求")
public class TranslationStartRequest {

    @NotNull(message = "文档ID不能为空")
    @Schema(description = "文档ID", example = "1")
    private Long documentId;

    @Schema(description = "源语言", example = "en", defaultValue = "en")
    private String sourceLang = "en";

    @NotNull(message = "目标语言不能为空")
    @Schema(description = "目标语言", example = "zh")
    private String targetLang;
}