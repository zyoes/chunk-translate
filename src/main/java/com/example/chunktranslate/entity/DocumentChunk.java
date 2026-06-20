package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.chunktranslate.common.entity.BaseEntity;
import lombok.Data;

/**
 * 文档分块实体，对应 document_chunk 表。
 * <p>文档解析后按段落/章节拆分为多个分块，通过 sequence 字段保持原始顺序。
 * 翻译时每个分块独立处理，状态参考 {@link com.example.chunktranslate.common.enums.ChunkStatus}。</p>
 */
@Data
@TableName("document_chunk")
public class DocumentChunk extends BaseEntity {
    /** 所属文档 ID */
    private Long documentId;

    /** 在文档中的顺序号，深度优先遍历生成，查询时按此排序可还原原文结构 */
    private Integer sequence;

    /** 分块标题（段落标题或推断的章节名） */
    private String title;

    /** 分块正文内容 */
    private String content;

    /** Token 数量估算值，用于控制翻译 API 的输入长度 */
    private Integer tokenCount;

    /** 翻译状态，对应 {@link com.example.chunktranslate.common.enums.ChunkStatus} */
    private Integer status;

    /** 翻译后的文本（润色后的最终译文） */
    private String translation;

    /** 翻译后的标题 */
    private String translatedTitle;

    /** 翻译失败时的错误信息 */
    private String errorMsg;

    /** 失败重试次数，最多 3 次 */
    private Integer retryCount;
}
