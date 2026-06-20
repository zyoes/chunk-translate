package com.example.chunktranslate.dto.document;

import lombok.Data;

import java.util.List;

/**
 * 文档树节点 DTO。
 * <p>表示解析后文档结构中的一个节点，支持多级嵌套（通过 children 字段）。
 * 叶子节点包含实际文本内容和 token 估算数。</p>
 */
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