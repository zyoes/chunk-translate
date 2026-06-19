package com.example.chunktranslate.service.translation;

import com.example.chunktranslate.dto.TranslationHistoryResponse;
import com.example.chunktranslate.dto.TranslationProgressResponse;
import com.example.chunktranslate.dto.TranslationStartRequest;
import com.example.chunktranslate.dto.TranslationTaskDetailResponse;

import java.util.List;

/**
 * 翻译服务接口
 * <p>
 * 提供翻译任务启动、进度查询等核心能力。
 * </p>
 *
 * <p>核心流程：</p>
 * <ol>
 *   <li>启动翻译：校验文档状态 → 将所有 chunk 标记为翻译中 → 异步并发执行翻译</li>
 *   <li>单块翻译：阿里云机器翻译（alimt）→ DeepSeek AI 润色 → 写入 translation_result 表</li>
 *   <li>进度查询：从 chunk 状态聚合计算整体进度百分比</li>
 * </ol>
 *
 * @see com.example.chunktranslate.service.translation.impl.TranslationServiceImpl
 * @see com.example.chunktranslate.service.translation.impl.TranslationTaskExecutor
 */
public interface TranslationService {

    /**
     * 启动翻译任务
     * <p>
     * 同步校验后异步执行翻译，立即返回初始进度响应。
     * 调用方可通过 {@link #getProgress(Long)} 轮询翻译进度。
     * </p>
     *
     * @param request 翻译请求（文档ID、源语言、目标语言）
     * @return 翻译进度初始响应
     * @throws com.example.chunktranslate.common.exception.BusinessException 文档不存在或翻译进行中时抛出
     */
    TranslationProgressResponse startTranslation(TranslationStartRequest request);

    /**
     * 查询翻译进度
     * <p>
     * 从 document_chunk 表聚合各分块状态，计算整体完成百分比。
     * COMPLETED 和 FAILED 均视为「已处理」。
     * </p>
     *
     * @param documentId 文档ID
     * @return 当前翻译进度（含各分块详情）
     * @throws com.example.chunktranslate.common.exception.BusinessException 文档不存在时抛出
     */
    TranslationProgressResponse getProgress(Long documentId);

    /**
     * 更新分块译文（校对编辑）
     * <p>
     * 用户在前端手动修改译文后调用，同时更新 document_chunk 和 translation_result 表。
     * </p>
     *
     * @param chunkId     分块ID
     * @param translation 修改后的译文
     * @throws com.example.chunktranslate.common.exception.BusinessException 分块不存在时抛出
     */
    void updateChunkTranslation(Long chunkId, String translation);

    /**
     * 更新分块原文（校对编辑）
     *
     * @param chunkId 分块ID
     * @param content 修改后的原文
     */
    void updateChunkSource(Long chunkId, String content);

    /**
     * 中止指定文档的翻译任务
     * <p>
     * 将取消标志置为 true，尚未开始执行的 chunk 将被跳过，
     * 正在执行中的 chunk 无法立即中断，会自然完成。
     * </p>
     *
     * @param documentId 文档ID
     */
    void stopTranslation(Long documentId);

    /**
     * 查询当前用户的翻译历史
     *
     * @return 翻译历史列表（按时间倒序）
     */
    List<TranslationHistoryResponse> getHistory();

    /**
     * 查询翻译任务详情
     *
     * @param taskId 翻译任务ID
     * @return 任务详情（含文档信息）
     */
    TranslationTaskDetailResponse getTaskDetail(Long taskId);

}
