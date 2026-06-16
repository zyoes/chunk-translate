package com.example.chunktranslate.service.document.parser;

import com.example.chunktranslate.dto.DocumentTreeNode;

import java.nio.file.Path;
import java.util.List;

public interface DocumentParser {

    /**
     * 解析文档，返回章节树
     */
    List<DocumentTreeNode> parse(Path filePath);

    /**
     * 当前解析器支持的文件类型（如 "pdf", "docx", "md"）
     */
    String supportedType();

}
