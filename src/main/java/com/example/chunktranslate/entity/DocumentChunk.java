package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.chunktranslate.common.entity.BaseEntity;
import lombok.Data;

@Data
@TableName("document_chunk")
public class DocumentChunk extends BaseEntity {
    private Long documentId;

    private Integer sequence;

    private String title;

    private String content;

    private Integer tokenCount;

    private Integer status;

    private String translation;

    private String translatedTitle;

    private String errorMsg;

    private Integer retryCount;
}
