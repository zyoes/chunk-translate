package com.example.chunktranslate.controller;

import com.example.chunktranslate.common.result.Result;
import com.example.chunktranslate.dto.TranslationProgressResponse;
import com.example.chunktranslate.dto.TranslationStartRequest;
import com.example.chunktranslate.service.translation.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 翻译管理控制器
 * <p>
 * 提供翻译任务启动、进度查询等 REST 接口。
 * </p>
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>POST /api/translation/start — 启动翻译任务（异步执行，立即返回）</li>
 *   <li>GET  /api/translation/progress/{documentId} — 查询翻译进度（支持前端轮询）</li>
 *   <li>PUT  /api/translation/chunk/{chunkId} — 校对编辑译文（用户手动修改）</li>
 *   <li>PUT  /api/translation/chunk/{chunkId}/source — 校对编辑原文（用户手动修改）</li>
 * </ul>
 */
@Tag(name = "翻译管理", description = "翻译任务启动、进度查询")
@RestController
@RequestMapping("/api/translation")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;

    /**
     * 启动翻译任务
     * <p>
     * 提交翻译请求后，系统在后台线程池异步并发执行翻译。
     * 接口立即返回初始进度信息，前端可通过 documentId 轮询进度接口。
     * </p>
     *
     * @param request 翻译请求（documentId、sourceLang、targetLang），@Valid 触发参数校验
     * @return 翻译进度初始响应（completedChunks=0, progressPercent=0）
     */
    @Operation(summary = "启动翻译任务", description = "异步执行翻译，立即返回初始进度信息")
    @PostMapping("/start")
    public Result<TranslationProgressResponse> startTranslation(
            @Valid @RequestBody TranslationStartRequest request) {
        return Result.success(translationService.startTranslation(request));
    }

    /**
     * 查询翻译进度
     * <p>
     * 返回文档整体翻译进度百分比，以及每个分块的详细翻译状态。
     * 前端可通过轮询该接口实现进度条实时更新。
     * </p>
     *
     * @param documentId 文档ID
     * @return 翻译进度响应（含百分比、各分块状态和译文）
     */
    @Operation(summary = "查询翻译进度", description = "返回翻译进度百分比及各分块详情")
    @GetMapping("/progress/{documentId}")
    public Result<TranslationProgressResponse> getProgress(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        return Result.success(translationService.getProgress(documentId));
    }

    /**
     * 校对编辑译文
     * <p>
     * 用户在前端手动修改译文后提交，同时更新 document_chunk 和 translation_result 表。
     * </p>
     *
     * @param chunkId 分块ID
     * @param body    请求体，包含 translation 字段
     * @return 操作结果
     */
    @Operation(summary = "校对编辑译文", description = "用户手动修改某分块的译文")
    @PutMapping("/chunk/{chunkId}")
    public Result<Void> updateChunkTranslation(
            @Parameter(description = "分块ID") @PathVariable Long chunkId,
            @RequestBody Map<String, String> body) {
        String translation = body.get("translation");
        translationService.updateChunkTranslation(chunkId, translation);
        return Result.success();
    }

    /**
     * 校对编辑原文
     *
     * @param chunkId 分块ID
     * @param body    请求体，包含 content 字段
     * @return 操作结果
     */
    @Operation(summary = "校对编辑原文", description = "用户手动修改某分块的原文内容")
    @PutMapping("/chunk/{chunkId}/source")
    public Result<Void> updateChunkSource(
            @Parameter(description = "分块ID") @PathVariable Long chunkId,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        translationService.updateChunkSource(chunkId, content);
        return Result.success();
    }

    /**
     * 中止翻译任务
     * <p>
     * 通知后台停止尚未执行的 chunk 翻译，正在执行中的 chunk 无法立即中断。
     * 未完成的 chunk 状态将回滚为「待翻译」。
     * </p>
     *
     * @param documentId 文档ID
     * @return 操作结果
     */
    @Operation(summary = "中止翻译任务", description = "停止指定文档的翻译任务，未完成 chunk 回滚为待翻译")
    @PostMapping("/stop/{documentId}")
    public Result<Void> stopTranslation(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        translationService.stopTranslation(documentId);
        return Result.success();
    }
}
