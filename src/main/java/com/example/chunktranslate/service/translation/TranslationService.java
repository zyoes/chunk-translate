package com.example.chunktranslate.service.translation;

import com.example.chunktranslate.dto.TranslationProgressResponse;
import com.example.chunktranslate.dto.TranslationStartRequest;

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
}
