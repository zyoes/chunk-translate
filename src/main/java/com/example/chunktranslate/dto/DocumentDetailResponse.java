package com.example.chunktranslate.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentDetailResponse {
    private Long id;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private Integer status;

    private String statusDesc;

    private Integer totalSections;

    private LocalDateTime createTime;

    private List<DocumentTreeNode> tree;
}