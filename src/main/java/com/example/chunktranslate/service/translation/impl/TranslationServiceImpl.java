package com.example.chunktranslate.service.translation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.chunktranslate.common.enums.ChunkStatus;
import com.example.chunktranslate.common.enums.DocumentStatus;
import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.dto.TranslationProgressResponse;
import com.example.chunktranslate.dto.TranslationStartRequest;
import com.example.chunktranslate.entity.Document;
import com.example.chunktranslate.entity.DocumentChunk;
import com.example.chunktranslate.entity.TranslationResult;
import com.example.chunktranslate.mapper.DocumentChunkMapper;
import com.example.chunktranslate.mapper.DocumentMapper;
import com.example.chunktranslate.mapper.TranslationResultMapper;
import com.example.chunktranslate.service.translation.TranslationService;
import com.example.chunktranslate.util.ParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 翻译服务实现类
 * <p>
 * 实现翻译任务启动和进度查询的业务逻辑。
 * 实际的翻译执行由 {@link TranslationTaskExecutor} 异步完成，
 * 确保 @Async 通过 Spring AOP 代理生效。
 * </p>
 *
 * <p>启动翻译流程（同步）：</p>
 * <ol>
 *   <li>校验文档是否存在</li>
 *   <li>检查是否有正在翻译的 chunk（防止重复提交）</li>
 *   <li>查询所有 chunk，标记为「翻译中」</li>
 *   <li>更新文档状态为「翻译中」</li>
 *   <li>通过 {@link TranslationTaskExecutor} 异步并发执行翻译</li>
 *   <li>立即返回初始进度响应</li>
 * </ol>
 *
 * @see TranslationTaskExecutor
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationServiceImpl implements TranslationService {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final TranslationResultMapper translationResultMapper;
    private final TranslationTaskExecutor translationTaskExecutor;
    private final ParserUtil parserUtil;

    /**
     * 启动翻译任务
     * <p>
     * 同步完成校验和状态标记，翻译在后台线程池并发执行。
     * </p>
     */
    @Override
    public TranslationProgressResponse startTranslation(TranslationStartRequest request) {
        // 1. 校验文档是否存在
        Document document = documentMapper.selectById(request.getDocumentId());
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }

        // 2. 检查是否正在翻译中（防止重复提交）
        List<DocumentChunk> translatingChunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, request.getDocumentId())
                        .eq(DocumentChunk::getStatus, ChunkStatus.TRANSLATING.getCode())
        );
        if (!translatingChunks.isEmpty()) {
            throw new BusinessException(ResultCode.TRANSLATION_IN_PROGRESS);
        }

        // 3. 查询所有 chunk（按序号排序）
        List<DocumentChunk> allChunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, request.getDocumentId())
                        .orderByAsc(DocumentChunk::getSequence)  // Bug修复：按序号排序，不是按状态
        );

        // 4. 标记所有 chunk 为「翻译中」
        LambdaUpdateWrapper<DocumentChunk> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DocumentChunk::getDocumentId, request.getDocumentId())
                .eq(DocumentChunk::getStatus, ChunkStatus.PENDING.getCode())
                .set(DocumentChunk::getStatus, ChunkStatus.TRANSLATING.getCode());
        documentChunkMapper.update(null, updateWrapper);

        // 重新查询所有 chunk（按序号排序）
        allChunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, request.getDocumentId())
                        .eq(DocumentChunk::getStatus, ChunkStatus.TRANSLATING.getCode())
                        .orderByAsc(DocumentChunk::getSequence)
        );

        // 5. 更新文档状态为「翻译中」
        document.setStatus(DocumentStatus.PARSING.getCode());
        documentMapper.updateById(document);  // Bug修复：补上 updateById，否则状态不会写入DB

        // 6. 通过独立的 Bean 调用异步方法（走 Spring 代理，@Async 生效）
        translationTaskExecutor.doTranslate(
                request.getDocumentId(), allChunks,
                request.getSourceLang(), request.getTargetLang());

        // 7. 立即返回初始进度响应（不等待翻译完成）
        TranslationProgressResponse response = new TranslationProgressResponse();
        response.setDocumentId(document.getId());
        response.setFileName(document.getFileName());
        response.setStatus(DocumentStatus.PARSING.getCode());
        response.setStatusDesc("翻译中");
        response.setTotalChunks(allChunks.size());
        response.setCompletedChunks(0);
        response.setProgressPercent(0);
        return response;
    }

    /**
     * 查询翻译进度
     * <p>
     * 从 document + document_chunk 聚合计算整体进度。
     * COMPLETED 和 FAILED 均视为「已处理」。
     * </p>
     */
    @Override
    public TranslationProgressResponse getProgress(Long documentId) {
        // 1. 查询文档
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }

        // 2. 查询所有 chunk（按序号排序）
        List<DocumentChunk> chunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)
                        .orderByAsc(DocumentChunk::getSequence)
        );

        // 3. 计算已处理数量（COMPLETED + FAILED 都算已处理）
        long completedCount = chunks.stream()
                .filter(chunk -> chunk.getStatus() == ChunkStatus.COMPLETED.getCode()
                        || chunk.getStatus() == ChunkStatus.FAILED.getCode())
                .count();

        // 4. 构建各 chunk 详情列表
        List<TranslationProgressResponse.ChunkTranslationInfo> chunkInfos = chunks.stream()
                .map(chunk -> {
                    TranslationProgressResponse.ChunkTranslationInfo info =
                            new TranslationProgressResponse.ChunkTranslationInfo();
                    info.setChunkId(chunk.getId());
                    info.setSequence(chunk.getSequence());
                    info.setTitle(chunk.getTitle());
                    info.setStatus(chunk.getStatus());
                    info.setStatusDesc(ChunkStatus.values()[chunk.getStatus()].getDesc());
                    info.setTokenCount(chunk.getTokenCount());
                    // 原文预览：截取前 100 字符
                    info.setSourcePreview(parserUtil.truncate(chunk.getContent(), 100));
                    info.setTranslation(chunk.getTranslation());
                    info.setErrorMsg(chunk.getErrorMsg());

                    // 翻译标题：直接从 chunk 的 translatedTitle 字段读取
                    if (chunk.getTranslatedTitle() != null && !chunk.getTranslatedTitle().isEmpty()) {
                        info.setTranslatedTitle(chunk.getTranslatedTitle());
                    }

                    return info;
                })
                .collect(Collectors.toList());

        // 5. 组装进度响应
        int total = chunks.size();
        int completed = (int) completedCount;

        TranslationProgressResponse response = new TranslationProgressResponse();
        response.setDocumentId(documentId);
        response.setFileName(document.getFileName());
        response.setStatus(document.getStatus());
        response.setStatusDesc(DocumentStatus.fromCode(document.getStatus()).getDesc());
        response.setTotalChunks(total);
        response.setCompletedChunks(completed);
        response.setProgressPercent(total == 0 ? 0 : (int) (completed * 100.0 / total));
        response.setChunks(chunkInfos);

        return response;
    }

    /**
     * 更新分块译文（校对编辑）
     * <p>
     * 同时更新 document_chunk.translation 和 translation_result.target_text。
     * </p>
     */
    @Override
    public void updateChunkTranslation(Long chunkId, String translation) {
        // 1. 查询分块
        DocumentChunk chunk = documentChunkMapper.selectById(chunkId);
        if (chunk == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }

        // 2. 更新 document_chunk 表的译文
        chunk.setTranslation(translation);
        documentChunkMapper.updateById(chunk);

        // 3. 更新 translation_result 表的译文（如果存在）
        TranslationResult result = translationResultMapper.selectOne(
                new LambdaQueryWrapper<TranslationResult>()
                        .eq(TranslationResult::getChunkId, chunkId)
                        .last("LIMIT 1")
        );
        if (result != null) {
            result.setTargetText(translation);
            translationResultMapper.updateById(result);
        }

        log.info("译文校对更新: chunkId={}, 新译文长度={}", chunkId, translation.length());
    }

    /**
     * 更新分块原文（校对编辑）
     * <p>
     * 同时更新 document_chunk.content 和 translation_result.source_text。
     * </p>
     */
    @Override
    public void updateChunkSource(Long chunkId, String content) {
        DocumentChunk chunk = documentChunkMapper.selectById(chunkId);
        if (chunk == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }

        chunk.setContent(content);
        documentChunkMapper.updateById(chunk);

        TranslationResult result = translationResultMapper.selectOne(
                new LambdaQueryWrapper<TranslationResult>()
                        .eq(TranslationResult::getChunkId, chunkId)
                        .last("LIMIT 1")
        );
        if (result != null) {
            result.setSourceText(content);
            translationResultMapper.updateById(result);
        }

        log.info("原文校对更新: chunkId={}, 新原文长度={}", chunkId, content.length());
    }

    /**
     * 中止指定文档的翻译任务
     * <p>
     * 通过 {@link TranslationTaskExecutor#cancelTranslation(Long)} 将取消标志置为 true，
     * 尚未开始执行的 chunk 将被跳过，正在执行中的 chunk 无法立即中断。
     * 同时将未完成的 chunk 状态回滚为待翻译，以便下次重新启动。
     * </p>
     */
    @Override
    public void stopTranslation(Long documentId) {
        // 1. 通知执行器取消
        translationTaskExecutor.cancelTranslation(documentId);

        // 2. 将仍处于「翻译中」状态的 chunk 回滚为「待翻译」
        LambdaUpdateWrapper<DocumentChunk> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DocumentChunk::getDocumentId, documentId)
                .eq(DocumentChunk::getStatus, ChunkStatus.TRANSLATING.getCode())
                .set(DocumentChunk::getStatus, ChunkStatus.PENDING.getCode());
        int rollbackCount = documentChunkMapper.update(null, updateWrapper);

        log.info("翻译任务已中止: documentId={}, 回滚chunk数={}", documentId, rollbackCount);
    }
}
