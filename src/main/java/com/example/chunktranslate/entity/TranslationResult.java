package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.chunktranslate.common.entity.BaseEntity;
import lombok.Data;

@Data
@TableName("translation_result")
public class TranslationResult extends BaseEntity {
    private Long documentId;

    private Long chunkId;

    private String sourceText;

    private String targetText;

    private String sourceLang;

    private String targetLang;

    private String style;

    private Integer status;
}
