package com.example.chunktranslate.service.document.impl;


import com.example.chunktranslate.common.enums.ChunkStatus;
import com.example.chunktranslate.common.enums.DocumentStatus;
import com.example.chunktranslate.dto.document.DocumentTreeNode;
import com.example.chunktranslate.entity.Document;
import com.example.chunktranslate.entity.DocumentChunk;
import com.example.chunktranslate.mapper.DocumentChunkMapper;
import com.example.chunktranslate.mapper.DocumentMapper;
import com.example.chunktranslate.service.document.parser.DocumentParser;
import com.example.chunktranslate.service.document.parser.ParserStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParseExecutor {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final ParserStrategyFactory parserStrategyFactory;

    /**
     * 异步解析文档（在 translationExecutor 线程池中执行）
     *
     * @param documentId 文档 ID
     * @param filePath   文件在磁盘上的绝对路径
     * @param fileType   文件类型（如 "pdf", "docx"）
     */
    @Async("translationExecutor")
    public void doParse(Long documentId, Path filePath, String fileType) {
        try {
            log.info("开始解析文档 [id={}, type={}]", documentId, fileType);

            // 根据文件类型从工厂获取对应的解析器实现
            DocumentParser parser = parserStrategyFactory.getParser(fileType);

            // 执行解析，得到章节树节点列表
            List<DocumentTreeNode> nodes = parser.parse(filePath);

            // 将树结构展平后逐个写入 document_chunk 表
            // flattenTree 采用深度优先遍历，确保父节点在子节点之前写入
            int sequence = 1;
            for (DocumentTreeNode node : flattenTree(nodes)) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(documentId);
                chunk.setSequence(sequence++);           // 递增序号，用于后续按序还原
                chunk.setTitle(node.getTitle());
                chunk.setContent(node.getContent());
                chunk.setTokenCount(node.getTokenCount() != null ? node.getTokenCount() : 0);
                chunk.setStatus(ChunkStatus.PENDING.getCode()); // 待翻译（使用 ChunkStatus 枚举）
                documentChunkMapper.insert(chunk);
            }

            // 更新文档状态为「已解析」
            Document updateDoc = new Document();
            updateDoc.setId(documentId);
            updateDoc.setStatus(DocumentStatus.PARSED.getCode());
            documentMapper.updateById(updateDoc);

            log.info("文档解析完成 [id={}, sections={}]", documentId, sequence - 1);

        } catch (Exception e) {
            log.error("文档解析失败 [id={}]", documentId, e);

            // 更新文档状态为「解析失败」
            Document updateDoc = new Document();
            updateDoc.setId(documentId);
            updateDoc.setStatus(DocumentStatus.PARSE_FAILED.getCode());
            documentMapper.updateById(updateDoc);
        }
    }

    /**
     * 将树结构展平为列表（深度优先遍历）
     * <p>
     * document_chunk 表是扁平存储的，树结构通过 sequence 顺序还原。
     * </p>
     */
    private List<DocumentTreeNode> flattenTree(List<DocumentTreeNode> nodes) {
        List<DocumentTreeNode> result = new ArrayList<>();
        for (DocumentTreeNode node : nodes) {
            result.add(node);
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                result.addAll(flattenTree(node.getChildren()));
            }
        }
        return result;
    }
}
