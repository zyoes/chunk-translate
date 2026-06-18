package com.example.chunktranslate.service.translation.impl;

import com.example.chunktranslate.common.enums.ChunkStatus;
import com.example.chunktranslate.common.enums.DocumentStatus;
import com.example.chunktranslate.common.enums.TranslationStatus;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.entity.Document;
import com.example.chunktranslate.entity.DocumentChunk;
import com.example.chunktranslate.entity.TranslationResult;
import com.example.chunktranslate.mapper.DocumentChunkMapper;
import com.example.chunktranslate.mapper.DocumentMapper;
import com.example.chunktranslate.mapper.TranslationResultMapper;
import com.example.chunktranslate.service.translation.ALimtTranslationClient;
import com.example.chunktranslate.service.translation.DeepSeekPolishClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 翻译任务异步执行器
 * <p>
 * 将每个 chunk 独立提交到 {@code translationExecutor} 线程池并发翻译，
 * 充分利用线程池资源实现真正的并发执行。
 * </p>
 *
 * <p>独立为单独的 Bean，确保 {@code @Async} 通过 Spring AOP 代理生效。
 * 如果在 TranslationServiceImpl 内部直接调用 @Async 方法，
 * 由于 {@code this} 调用不走代理，异步会退化为同步。</p>
 *
 * <p>翻译流水线（每个 chunk）：</p>
 * <ol>
 *   <li>阿里云机器翻译（alimt）— 保证翻译准确性</li>
 *   <li>DeepSeek AI 润色 — 提升自然度和可读性</li>
 *   <li>写入 translation_result 表 + 更新 chunk 状态为 COMPLETED</li>
 * </ol>
 *
 * <p>重试机制：每个 chunk 最多重试 {@value #MAX_RETRY} 次，失败后标记为 FAILED 并记录错误信息。</p>
 *
 * @see TranslationServiceImpl
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationTaskExecutor {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final TranslationResultMapper translationResultMapper;
    private final ALimtTranslationClient alimtClient;
    private final DeepSeekPolishClient deepSeekPolishClient;

    /** 每个 chunk 的最大重试次数 */
    private static final int MAX_RETRY = 3;

    /**
     * 并发翻译所有 chunk
     * <p>
     * 每个 chunk 通过 {@link #translateSingleChunk} 独立异步执行，
     * 线程池（translationExecutor）负责并发调度。
     * 当所有 chunk 都处理完毕后，自动更新文档最终状态。
     * </p>
     *
     * @param documentId 文档ID
     * @param chunks     所有待翻译的分块列表
     * @param sourceLang 源语言代码（如 "en"）
     * @param targetLang 目标语言代码（如 "zh"）
     */
    public void doTranslate(Long documentId, List<DocumentChunk> chunks,
                            String sourceLang, String targetLang) {
        log.info("开始翻译任务: documentId={}, 总chunk数={}", documentId, chunks.size());

        // AtomicInteger：线程安全的计数器，多个线程同时 +1 不会丢数据
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        int total = chunks.size();

        // 每个 chunk 单独提交到线程池异步执行（真正实现并发）
        for (DocumentChunk chunk : chunks) {
            translateSingleChunk(chunk, sourceLang, targetLang)
                    .thenAccept(success -> {
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }

                        // 当所有 chunk 都处理完毕时，更新文档最终状态
                        if (successCount.get() + failCount.get() == total) {
                            updateDocumentStatus(documentId, failCount.get() == 0);
                        }
                    });
        }
    }

    /**
     * 异步翻译单个 chunk
     * <p>
     * 每个 chunk 独立在线程池中执行，实现真正的并发。
     * 翻译流程：alimt 机器翻译 → DeepSeek AI 润色 → 写入 translation_result → 更新 chunk 状态。
     * 失败时最多重试 {@value #MAX_RETRY} 次。
     * </p>
     *
     * @param chunk      待翻译的分块
     * @param sourceLang 源语言代码
     * @param targetLang 目标语言代码
     * @return CompletableFuture 包装的翻译结果（true=成功，false=失败）
     */
    @Async("translationExecutor")
    public CompletableFuture<Boolean> translateSingleChunk(
            DocumentChunk chunk, String sourceLang, String targetLang) {
        int retryCount = 0;

        while (retryCount < MAX_RETRY) {
            try {
                // 第一级：阿里云机器翻译（保证准确性）
                String machineTranslation = alimtClient.translate(
                        chunk.getContent(), sourceLang, targetLang);

                // 第二级：DeepSeek AI 润色（提升流畅度）
                String polished = deepSeekPolishClient.polish(
                        chunk.getContent(), machineTranslation, sourceLang, targetLang);

                // 写入翻译结果表（记录原文和译文，方便后续查看对照）
                TranslationResult result = new TranslationResult();
                result.setDocumentId(chunk.getDocumentId());
                result.setChunkId(chunk.getId());
                result.setSourceText(chunk.getContent());
                result.setTargetText(polished);
                result.setSourceLang(sourceLang);
                result.setTargetLang(targetLang);
                result.setStatus(TranslationStatus.COMPLETED.getCode());
                translationResultMapper.insert(result);

                // 更新 chunk 状态为已完成
                chunk.setStatus(ChunkStatus.COMPLETED.getCode());
                chunk.setTranslation(polished);
                chunk.setRetryCount(retryCount);
                documentChunkMapper.updateById(chunk);

                log.debug("chunk翻译成功: chunkId={}, title={}", chunk.getId(), chunk.getTitle());
                return CompletableFuture.completedFuture(true);

            } catch (Exception e) {
                retryCount++;
                log.warn("chunk翻译失败, 重试: chunkId={}, retry={}/{}",
                        chunk.getId(), retryCount, MAX_RETRY);

                if (retryCount >= MAX_RETRY) {
                    // 重试耗尽，标记为失败
                    chunk.setStatus(ChunkStatus.FAILED.getCode());
                    chunk.setErrorMsg(e.getMessage());
                    chunk.setRetryCount(retryCount);
                    documentChunkMapper.updateById(chunk);

                    log.error("chunk翻译最终失败: chunkId={}, title={}", chunk.getId(), chunk.getTitle(), e);
                    return CompletableFuture.completedFuture(false);
                }
            }
        }
        return CompletableFuture.completedFuture(false);
    }

    /**
     * 所有 chunk 处理完毕后更新文档最终状态
     *
     * @param documentId 文档ID
     * @param allSuccess 是否全部翻译成功
     */
    private void updateDocumentStatus(Long documentId, boolean allSuccess) {
        Document document = documentMapper.selectById(documentId);
        document.setStatus(allSuccess
                ? DocumentStatus.PARSED.getCode()
                : DocumentStatus.PARSE_FAILED.getCode());
        documentMapper.updateById(document);

        log.info("翻译任务完成: documentId={}, 全部成功={}", documentId, allSuccess);
    }
}
