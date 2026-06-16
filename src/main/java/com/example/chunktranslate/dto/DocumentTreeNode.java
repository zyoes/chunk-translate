package com.example.chunktranslate.dto;

import lombok.Data;

import java.util.List;

@Data
public class DocumentTreeNode {
    private String nodeId;

    private String title;

    private Integer level;

    private String summary;

    private String content;

    private Integer tokenCount;

    private List<DocumentTreeNode> children;
}