package com.example.chunktranslate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 翻译任务详情响应 DTO，聚合翻译任务信息和文档详情。
 */
@Data
@Schema(description = "翻译任务详情")
public class TranslationTaskDetailResponse {

    @Schema(description = "翻译任务")
    private TranslationHistoryResponse task;

    @Schema(description = "文档详情")
    private DocumentDetailResponse document;
}
