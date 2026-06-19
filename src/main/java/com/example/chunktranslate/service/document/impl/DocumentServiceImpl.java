package com.example.chunktranslate.service.document.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.chunktranslate.common.enums.DocumentStatus;
import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.dto.DocumentDetailResponse;
import com.example.chunktranslate.dto.DocumentTreeNode;
import com.example.chunktranslate.dto.DocumentUploadResponse;
import com.example.chunktranslate.entity.Document;
import com.example.chunktranslate.entity.DocumentChunk;
import com.example.chunktranslate.mapper.DocumentChunkMapper;
import com.example.chunktranslate.mapper.DocumentMapper;
import com.example.chunktranslate.service.document.DocumentService;
import com.example.chunktranslate.service.document.parser.DocumentParser;
import com.example.chunktranslate.service.document.parser.ParserStrategyFactory;
import com.example.chunktranslate.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理服务实现类
 * <p>
 * 实现文档上传、异步解析、详情查询的完整业务逻辑。
 * </p>
 *
 * <p>上传流程（同步）：</p>
 * <ol>
 *   <li>提取文件扩展名，通过 {@link ParserStrategyFactory} 校验类型是否支持</li>
 *   <li>通过 {@link FileStorageUtil} 将文件持久化到磁盘（按日期分目录）</li>
 *   <li>创建 {@link Document} 记录写入数据库，初始状态 = {@link DocumentStatus#PARSING}</li>
 *   <li>调用 {@link DocumentParseExecutor#doParse} 异步触发解析</li>
 *   <li>立即返回上传响应（不等待解析完成）</li>
 * </ol>
 *
 * <p>解析流程（异步，在 translationExecutor 线程池中执行）：</p>
 * <ol>
 *   <li>通过 {@link ParserStrategyFactory} 路由到对应 {@link DocumentParser} 实现</li>
 *   <li>执行解析得到章节树，展平后写入 {@link DocumentChunk} 表</li>
 *   <li>成功：更新文档状态为 {@link DocumentStatus#PARSED}</li>
 *   <li>失败：更新文档状态为 {@link DocumentStatus#PARSE_FAILED}</li>
 * </ol>
 *
 * @see DocumentService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final FileStorageUtil fileStorageUtil;
    private final ParserStrategyFactory parserStrategyFactory;
    private final DocumentParseExecutor documentParseExecutor;

    /**
     * 上传并解析文档
     * <p>
     * 同步完成文件落盘 + DB 写入，然后触发异步解析。
     * </p>
     *
     * @param file 上传的文件
     * @return 上传响应
     */
    @Override
    public DocumentUploadResponse uploadAndParse(MultipartFile file) {
        // 1. 提取文件扩展名（小写），用于后续路由到对应解析器
        String originalFilename = file.getOriginalFilename();
        String filetype = getFileExtension(originalFilename);

        // 2. 校验文件类型是否支持（若不支持，getParser 内部会抛出 FILE_TYPE_NOT_SUPPORT 异常）
        parserStrategyFactory.getParser(filetype);

        // 3. 持久化文件到磁盘（按日期分目录存储，返回相对路径）
        String savedPath = fileStorageUtil.store(file);

        // 4. 创建文档记录，初始状态设为「解析中」
        Document document = new Document();
        document.setFileName(originalFilename);
        document.setFilePath(savedPath);        // 存储相对路径，便于迁移
        document.setFileType(filetype);
        document.setStatus(DocumentStatus.PARSING.getCode());
        document.setFileSize(file.getSize());
        documentMapper.insert(document);

        // 5. 触发解析任务：getFullPath() 将相对路径转为绝对路径给解析器使用
        documentParseExecutor.doParse(document.getId(), fileStorageUtil.getFullPath(savedPath), filetype);

        // 6. 构建并返回上传响应（此时解析可能尚未完成）
        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setFileType(filetype);
        response.setFileSize(file.getSize());
        response.setStatus(DocumentStatus.PARSING.getCode());       // 状态码
        response.setStatusDesc(DocumentStatus.PARSING.getDesc());   // 状态描述

        return response;
    }

    /**
     * 获取文档详情（含章节树）
     *
     * @param documentId 文档 ID
     * @return 文档详情响应
     * @throws BusinessException 文档不存在时抛出
     */
    @Override
    public DocumentDetailResponse getDocumentDetail(Long documentId) {
        // 1. 查询文档基本信息
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }

        // 2. 查询该文档的所有章节（LambdaQueryWrapper 支持链式条件拼接）
        List<DocumentChunk> chunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)    // WHERE document_id = ?
                        .orderByAsc(DocumentChunk::getSequence)          // ORDER BY sequence ASC
        );

        // 3. 将数据库章节实体转换为前端需要的树节点 DTO
        List<DocumentTreeNode> tree = chunks.stream()
                .map(this::chunkToTreeNode)   // 方法引用，等价于 chunk -> chunkToTreeNode(chunk)
                .toList();

        // 4. 组装响应对象
        DocumentDetailResponse response = new DocumentDetailResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setFileType(document.getFileType());
        response.setFileSize(document.getFileSize());
        response.setStatus(document.getStatus());
        // 将 int 状态码转换为中文描述（如 1 → "解析中"）
        response.setStatusDesc(DocumentStatus.fromCode(document.getStatus()).getDesc());
        response.setTotalSections(chunks.size());
        response.setCreateTime(document.getCreatedAt());
        response.setTree(tree);

        return response;
    }

    /**
     * 从文件名提取扩展名（小写）
     *
     * @param filename 原始文件名
     * @return 扩展名（不含点号），如 "pdf", "docx"
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORT);
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 将 DocumentChunk 实体转换为 DocumentTreeNode DTO
     */
    private DocumentTreeNode chunkToTreeNode(DocumentChunk chunk) {
        DocumentTreeNode node = new DocumentTreeNode();
        node.setNodeId(chunk.getId().toString());
        node.setTitle(chunk.getTitle());
        node.setLevel(1);
        node.setContent(chunk.getContent());
        node.setTokenCount(chunk.getTokenCount());
        node.setChildren(new java.util.ArrayList<>());
        return node;
    }
}
