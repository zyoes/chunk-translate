package com.example.chunktranslate.service.translation.impl;

import com.example.chunktranslate.common.enums.ChunkStatus;
import com.example.chunktranslate.common.enums.DocumentStatus;
import com.example.chunktranslate.common.enums.TranslationStatus;
import com.example.chunktranslate.entity.Document;
import com.example.chunktranslate.entity.DocumentChunk;
import com.example.chunktranslate.entity.TranslationResult;
import com.example.chunktranslate.mapper.DocumentChunkMapper;
import com.example.chunktranslate.mapper.DocumentMapper;
import com.example.chunktranslate.mapper.TranslationResultMapper;
import com.example.chunktranslate.service.translation.ALimtTranslationClient;
import com.example.chunktranslate.service.translation.DeepSeekPolishClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 翻译任务异步执行器
 * <p>
 * 将每个 chunk 独立提交到 {@code translationExecutor} 线程池并发翻译，
 * 充分利用线程池资源实现真正的并发执行。
 * </p>
 *
 * <p>通过手动注入 {@code translationExecutor} 线程池，使用 {@link CompletableFuture#supplyAsync}
 * 将每个 chunk 独立提交到线程池并发执行，彻底避免 {@code @Async} 自调用导致代理失效的问题。</p>
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
public class TranslationTaskExecutor {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final TranslationResultMapper translationResultMapper;
    private final ALimtTranslationClient alimtClient;
    private final DeepSeekPolishClient deepSeekPolishClient;
    private final Executor translationExecutor;

    public TranslationTaskExecutor(
            DocumentMapper documentMapper,
            DocumentChunkMapper documentChunkMapper,
            TranslationResultMapper translationResultMapper,
            ALimtTranslationClient alimtClient,
            DeepSeekPolishClient deepSeekPolishClient,
            @Qualifier("translationExecutor") Executor translationExecutor) {
        this.documentMapper = documentMapper;
        this.documentChunkMapper = documentChunkMapper;
        this.translationResultMapper = translationResultMapper;
        this.alimtClient = alimtClient;
        this.deepSeekPolishClient = deepSeekPolishClient;
        this.translationExecutor = translationExecutor;
    }

    /**
     * 翻译任务取消标志映射表
     * <p>
     * key: documentId，value: 取消标志（AtomicBoolean 保证线程安全）。
     * 调用 {@link #cancelTranslation(Long)} 后将 flag 置为 true，
     * 各 chunk 翻译前检查此标志，若已取消则跳过执行。
     * </p>
     */
    private final ConcurrentHashMap<Long, AtomicBoolean> cancelledTasks = new ConcurrentHashMap<>();

    /** DeepSeek 润色的最大文本长度（超过则跳过润色，直接用机翻译文） */
    private static final int MAX_POLISH_LENGTH = 8000;

    /** 每个 chunk 的最大重试次数 */
    private static final int MAX_RETRY = 3;

    /**
     * 并发翻译所有 chunk
     * <p>
     * 每个 chunk 通过 {@link CompletableFuture#supplyAsync} 提交到线程池并发执行，
     * 彻底绕过 {@code @Async} 自调用代理失效问题。
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

        // 初始化取消标志（false = 未取消）
        AtomicBoolean cancelled = cancelledTasks.computeIfAbsent(documentId, k -> new AtomicBoolean(false));

        // AtomicInteger：线程安全的计数器，多个线程同时 +1 不会丢数据
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        int total = chunks.size();

        // 每个 chunk 通过 supplyAsync 提交到线程池，真正实现并发
        for (DocumentChunk chunk : chunks) {
            CompletableFuture.supplyAsync(
                    () -> {
                        // 翻译前检查取消标志
                        if (cancelled.get()) {
                            log.info("任务已取消，跳过chunk: documentId={}, chunkId={}", documentId, chunk.getId());
                            return null;  // null 表示已取消，不计入成功/失败
                        }
                        return translateSingleChunk(chunk, sourceLang, targetLang);
                    },
                    translationExecutor
            ).thenAccept(success -> {
                        if (success == null) {
                            // 已取消的 chunk，不计数
                            return;
                        }
                        if (success) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }

                        // 当所有 chunk 都处理完毕时，更新文档最终状态
                        if (successCount.get() + failCount.get() == total) {
                            updateDocumentStatus(documentId, failCount.get() == 0);
                            cancelledTasks.remove(documentId);  // 清理标志
                        }
                    });
        }
    }

    /**
     * 取消指定文档的翻译任务
     * <p>
     * 将取消标志置为 true，已提交但尚未执行的 chunk 将在开始前检查此标志并跳过。
     * 正在执行中的 chunk 无法中断，会自然完成。
     * </p>
     *
     * @param documentId 文档ID
     */
    public void cancelTranslation(Long documentId) {
        AtomicBoolean cancelled = cancelledTasks.get(documentId);
        if (cancelled != null) {
            cancelled.set(true);
            log.info("翻译任务已取消: documentId={}", documentId);
        }
    }

    /**
     * 检查指定文档的翻译任务是否已取消
     *
     * @param documentId 文档ID
     * @return true=已取消，false=未取消或不存在
     */
    public boolean isCancelled(Long documentId) {
        AtomicBoolean cancelled = cancelledTasks.get(documentId);
        return cancelled != null && cancelled.get();
    }

    /**
     * 翻译单个 chunk（在线程池中执行）
     * <p>
     * 翻译流程：alimt 机器翻译 → DeepSeek AI 润色 → 写入 translation_result → 更新 chunk 状态。
     * 失败时最多重试 {@value #MAX_RETRY} 次。
     * 使用 {@code catch (Throwable)} 确保 {@link Error} 类型异常也能被正确处理。
     * </p>
     *
     * @param chunk      待翻译的分块
     * @param sourceLang 源语言代码
     * @param targetLang 目标语言代码
     * @return true=成功，false=失败
     */
    public Boolean translateSingleChunk(
            DocumentChunk chunk, String sourceLang, String targetLang) {

        // 空内容防护：分块只有标题没有正文时，跳过翻译 API 调用，直接标记为已完成
        if (chunk.getContent() == null || chunk.getContent().isBlank()) {
            log.info("chunk内容为空(仅有标题), 跳过翻译: chunkId={}, title={}", chunk.getId(), chunk.getTitle());

            TranslationResult result = new TranslationResult();
            result.setDocumentId(chunk.getDocumentId());
            result.setChunkId(chunk.getId());
            result.setSourceText("");
            result.setTargetText("");
            result.setSourceLang(sourceLang);
            result.setTargetLang(targetLang);
            result.setStatus(TranslationStatus.COMPLETED.getCode());
            translationResultMapper.insert(result);

            chunk.setStatus(ChunkStatus.COMPLETED.getCode());
            chunk.setTranslation("");
            chunk.setRetryCount(0);
            documentChunkMapper.updateById(chunk);
            return true;
        }

        // 标题拼入翻译：对于“段落N - ...”格式的标题，将标题放在内容前面一起翻译
        // 翻译后可从结果中提取翻译标题
        String title = chunk.getTitle();
        boolean hasTitlePrefix = title != null
                && !title.isBlank()
                && title.matches("段落\\s*\\d+\\s*-.*")
                && !chunk.getContent().trim().startsWith(title);
        String contentForTranslation = hasTitlePrefix
                ? title + "\n\n" + chunk.getContent()
                : chunk.getContent();

        int retryCount = 0;

        while (retryCount < MAX_RETRY) {
            try {
                // 第一级：阿里云机器翻译（保证准确性）
                String machineTranslation = alimtClient.translate(
                        contentForTranslation, sourceLang, targetLang);

                log.info("alimt翻译结果: chunkId={}, 原文长度={}, 译文长度={}, 译文前100字={}",
                        chunk.getId(),
                        contentForTranslation.length(),
                        machineTranslation != null ? machineTranslation.length() : "null",
                        machineTranslation != null && machineTranslation.length() > 100
                                ? machineTranslation.substring(0, 100) : machineTranslation);

                // 第二级：DeepSeek AI 润色（文本太长时跳过，直接用机翻译文）
                String polished;
                if (machineTranslation != null && machineTranslation.length() <= MAX_POLISH_LENGTH) {
                    polished = deepSeekPolishClient.polish(
                            contentForTranslation, machineTranslation, sourceLang, targetLang);
                } else {
                    log.info("译文过长({}字符), 跳过AI润色, 直接使用机翻译文",
                            machineTranslation != null ? machineTranslation.length() : 0);
                    polished = machineTranslation;
                }

                // 润色结果空值防护：若 DeepSeek 返回空内容，回退到机翻译文
                if (polished == null || polished.isBlank()) {
                    log.warn("润色结果为空, 回退到机翻译文: chunkId={}, machineTranslation长度={}",
                            chunk.getId(), machineTranslation != null ? machineTranslation.length() : 0);
                    polished = machineTranslation;
                }
                // 机翻译文也为空时，直接使用原文（避免存储 null）
                if (polished == null || polished.isBlank()) {
                    log.warn("机翻译文也为空, 直接使用原文: chunkId={}", chunk.getId());
                    polished = contentForTranslation;
                }

                // 提取翻译标题：若之前拼入了标题，从译文开头剥离翻译后的标题部分
                String translationBody = polished;
                String extractedTitle = null;
                if (hasTitlePrefix && polished != null) {
                    // 方法1: 按双换行分割（最可靠）
                    String[] parts = polished.split("\n\n", 2);
                    if (parts.length == 2) {
                        extractedTitle = parts[0].trim();
                        translationBody = parts[1].trim();
                        log.info("提取翻译标题(双换行): chunkId={}, 译后标题={}", chunk.getId(), extractedTitle);
                    } else {
                        // 方法2: 按第一个换行分割
                        int firstNl = polished.indexOf('\n');
                        if (firstNl > 0 && firstNl < 200) {
                            extractedTitle = polished.substring(0, firstNl).trim();
                            translationBody = polished.substring(firstNl + 1).trim();
                            log.info("提取翻译标题(单换行): chunkId={}, 译后标题={}", chunk.getId(), extractedTitle);
                        } else {
                            // 方法3: 基于原文标题字符数的启发式提取
                            // 原标题占拼入内容的比例，乘以译文长度，估算译后标题结束位置
                            int originalTitleLen = title.length();
                            int totalInputLen = contentForTranslation.length();
                            int estimatedEnd = (int) ((double) originalTitleLen / totalInputLen * polished.length());
                            // 在估算位置附近查找句末分隔符（句号、逗号、分号等）
                            int bestSplit = -1;
                            int searchStart = Math.max(5, estimatedEnd - 40);
                            int searchEnd = Math.min(polished.length() - 1, estimatedEnd + 60);
                            for (int i = searchStart; i < searchEnd; i++) {
                                char c = polished.charAt(i);
                                if (c == '.' || c == ',' || c == ';' || c == ':' || c == '\n') {
                                    if (bestSplit < 0 || Math.abs(i - estimatedEnd) < Math.abs(bestSplit - estimatedEnd)) {
                                        bestSplit = i;
                                    }
                                }
                            }
                            if (bestSplit > 0) {
                                extractedTitle = polished.substring(0, bestSplit + 1).trim();
                                translationBody = polished.substring(bestSplit + 1).trim();
                                log.info("提取翻译标题(启发式): chunkId={}, 译后标题={}, 估算位置={}", chunk.getId(), extractedTitle, estimatedEnd);
                            } else {
                                log.warn("无法提取翻译标题: chunkId={}, polished前100字={}", chunk.getId(),
                                        polished.length() > 100 ? polished.substring(0, 100) : polished);
                            }
                        }
                    }
                }

                // 写入翻译结果表（记录原文和完整译文，包含翻译标题）
                TranslationResult result = new TranslationResult();
                result.setDocumentId(chunk.getDocumentId());
                result.setChunkId(chunk.getId());
                result.setSourceText(chunk.getContent());
                result.setTargetText(polished);  // 完整译文（含标题）
                result.setSourceLang(sourceLang);
                result.setTargetLang(targetLang);
                result.setStatus(TranslationStatus.COMPLETED.getCode());
                translationResultMapper.insert(result);

                // 更新 chunk 状态为已完成（只存正文译文，不含标题）
                chunk.setStatus(ChunkStatus.COMPLETED.getCode());
                chunk.setTranslation(translationBody);
                chunk.setTranslatedTitle(extractedTitle);  // 存储翻译后的标题
                chunk.setRetryCount(retryCount);
                documentChunkMapper.updateById(chunk);

                log.info("chunk翻译成功: chunkId={}, title={}", chunk.getId(), chunk.getTitle());
                return true;

            } catch (Throwable e) {
                retryCount++;
                log.warn("chunk翻译失败, 重试: chunkId={}, retry={}/{}",
                        chunk.getId(), retryCount, MAX_RETRY, e);

                if (retryCount >= MAX_RETRY) {
                    // 重试耗尽，标记为失败
                    chunk.setStatus(ChunkStatus.FAILED.getCode());
                    chunk.setErrorMsg(e.getMessage());
                    chunk.setRetryCount(retryCount);
                    documentChunkMapper.updateById(chunk);

                    log.error("chunk翻译最终失败: chunkId={}, title={}", chunk.getId(), chunk.getTitle(), e);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 所有 chunk 处理完毕后更新文档最终状态
     *
     * @param documentId 文档ID
     * @param allSuccess 是否全部翻译成功
     */
    private void updateDocumentStatus(Long documentId, boolean allSuccess) {
        Document document = documentMapper.selectById(documentId);
        document.setStatus(DocumentStatus.PARSED.getCode());

        documentMapper.updateById(document);

        log.info("翻译任务完成: documentId={}, 全部成功={}", documentId, allSuccess);
    }
}
