package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.chunktranslate.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 翻译任务实体，对应 translation_task 表。
 * <p>每个文档的每次翻译请求创建一个任务，记录整体进度。
 * 分块级别的翻译状态在 {@link DocumentChunk} 和 {@link TranslationResult} 中追踪。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("translation_task")
public class TranslationTask extends BaseEntity {

    /** 发起翻译的用户 ID */
    private Long userId;

    /** 待翻译的文档 ID */
    private Long documentId;

    /** 源语言代码（如 zh/cn, en） */
    private String sourceLang;

    /** 目标语言代码 */
    private String targetLang;

    /** 任务状态，对应 {@link com.example.chunktranslate.common.enums.TaskStatus} */
    private Integer status;

    /** 文档总分块数 */
    private Integer totalChunks;

    /** 已完成翻译的分块数 */
    private Integer completedChunks;

    /** 翻译开始时间 */
    private LocalDateTime startedAt;

    /** 翻译完成时间 */
    private LocalDateTime completedAt;

    /** 文档文件名（仅后台管理列表使用，非数据库字段） */
    @TableField(exist = false)
    private String documentName;

    /** 上传者用户名（仅后台管理列表使用，非数据库字段） */
    @TableField(exist = false)
    private String uploaderName;
}
