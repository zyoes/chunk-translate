package com.example.chunktranslate.dto.translation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 翻译进度响应 DTO
 */
@Data
@Schema(description = "翻译进度响应")
public class TranslationProgressResponse {

    @Schema(description = "文档ID")
    private Long documentId;

    @Schema(description = "文档名")
    private String fileName;

    @Schema(description = "文档状态码")
    private Integer status;

    @Schema(description = "文档状态描述")
    private String statusDesc;

    @Schema(description = "总分块数")
    private Integer totalChunks;

    @Schema(description = "已完成分块数")
    private Integer completedChunks;

    @Schema(description = "翻译进度百分比（0~100）")
    private Integer progressPercent;

    @Schema(description = "各分块翻译详情")
    private List<ChunkTranslationInfo> chunks;

    /**
     * 单个分块的翻译状态信息
     */
    @Data
    @Schema(description = "分块翻译详情")
    public static class ChunkTranslationInfo {

        @Schema(description = "分块ID")
        private Long chunkId;

        @Schema(description = "分块序号")
        private Integer sequence;

        @Schema(description = "分块标题")
        private String title;

        @Schema(description = "状态码")
        private Integer status;

        @Schema(description = "状态描述")
        private String statusDesc;

        @Schema(description = "预估Token数")
        private Integer tokenCount;

        @Schema(description = "原文（截断前100字）")
        private String sourcePreview;

        @Schema(description = "译文（翻译完成后才有）")
        private String translation;

        @Schema(description = "翻译后的标题（从译文中提取）")
        private String translatedTitle;

        @Schema(description = "失败原因（仅失败时有值）")
        private String errorMsg;
    }

    @Schema(description = "翻译任务ID")
    private Long taskId;

}