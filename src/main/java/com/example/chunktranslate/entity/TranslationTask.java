package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.chunktranslate.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("translation_task")
public class TranslationTask extends BaseEntity {

    private Long userId;

    private Long documentId;

    private String sourceLang;

    private String targetLang;

    private Integer status;

    private Integer totalChunks;

    private Integer completedChunks;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}
