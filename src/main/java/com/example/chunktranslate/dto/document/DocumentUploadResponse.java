package com.example.chunktranslate.dto.document;

import lombok.Data;

/**
 * 文档上传响应 DTO，返回上传后的文档元数据。
 */
@Data
public class DocumentUploadResponse {
    private Long id;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private Integer status;

    private String statusDesc;
}
