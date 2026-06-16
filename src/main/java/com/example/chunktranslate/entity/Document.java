package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.chunktranslate.common.entity.BaseEntity;
import lombok.Data;

@Data
@TableName("document")
public class Document extends BaseEntity {
    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileType;

    private Integer status;
}
