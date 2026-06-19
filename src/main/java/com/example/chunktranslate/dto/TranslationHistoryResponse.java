package com.example.chunktranslate.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "翻译历史列表项")
public class TranslationHistoryResponse {

    @Schema(description = "翻译任务ID")
    private Long taskId;

    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "文档名")
    private String documentName;

    @Schema(description = "源语言")
    private String sourceLang;

    @Schema(description = "目标语言")
    private String targetLang;

    @Schema(description = "状态: 0-进行中 1-已完成 2-失败")
    private Integer status;

    @Schema(description = "总分块数")
    private Integer totalChunks;

    @Schema(description = "已完成分块数")
    private Integer completedChunks;

    @Schema(description = "进度百分比")
    private Integer progressPercent;

    @Schema(description = "开始时间")
    private LocalDateTime startedAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
