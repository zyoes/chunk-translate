package com.example.chunktranslate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "翻译任务详情")
public class TranslationTaskDetailResponse {

    @Schema(description = "翻译任务")
    private TranslationHistoryResponse task;

    @Schema(description = "文档详情")
    private DocumentDetailResponse document;
}
