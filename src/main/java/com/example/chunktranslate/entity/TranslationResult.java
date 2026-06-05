package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("translation_result")
public class TranslationResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;
    private Long chunkId;
    private String sourceText;
    private String targetText;
    private String sourceLang;
    private String targetLang;
    private String style;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
