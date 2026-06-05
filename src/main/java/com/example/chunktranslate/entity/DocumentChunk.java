package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_chunk")
public class DocumentChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;
    private Integer sequence;
    private String title;
    private String content;
    private Integer tokenCount;
    private Integer status;
    private String translation;
    private String errorMsg;
    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
