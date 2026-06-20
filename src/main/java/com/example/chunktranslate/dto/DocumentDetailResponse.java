package com.example.chunktranslate.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档详情响应 DTO，包含文档元数据和解析后的目录树结构。
 */
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