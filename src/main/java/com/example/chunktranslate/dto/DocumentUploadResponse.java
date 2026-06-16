package com.example.chunktranslate.dto;

import lombok.Data;

@Data
public class DocumentUploadResponse {
    private Long id;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private Integer status;

    private String statusDesc;
}
