package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.chunktranslate.common.entity.BaseEntity;
import lombok.Data;

/**
 * 翻译结果实体，对应 translation_result 表。
 * <p>每个分块的每次翻译（机翻或润色）都会生成一条记录，状态参考 {@link com.example.chunktranslate.common.enums.TranslationStatus}。</p>
 */
@Data
@TableName("translation_result")
public class TranslationResult extends BaseEntity {
    /** 所属文档 ID */
    private Long documentId;

    /** 所属分块 ID */
    private Long chunkId;

    /** 原文文本 */
    private String sourceText;

    /** 译文文本 */
    private String targetText;

    /** 源语言代码 */
    private String sourceLang;

    /** 目标语言代码 */
    private String targetLang;

    /** 翻译风格（如 formal/informal） */
    private String style;

    /** 翻译状态，对应 {@link com.example.chunktranslate.common.enums.TranslationStatus} */
    private Integer status;
}
