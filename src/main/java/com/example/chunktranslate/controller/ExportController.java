package com.example.chunktranslate.controller;

import com.example.chunktranslate.service.export.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 导出管理控制器
 * <p>
 * 提供译文导出接口，支持 TXT、Markdown、DOCX、PDF 四种格式。
 * 所有接口均通过 HttpServletResponse 输出流触发浏览器文件下载。
 * </p>
 *
 * <p>接口列表：</p>
 * <ul>
 *   <li>GET /api/export/txt/{documentId} — 导出为纯文本</li>
 *   <li>GET /api/export/markdown/{documentId} — 导出为 Markdown</li>
 *   <li>GET /api/export/docx/{documentId} — 导出为 Word 文档</li>
 *   <li>GET /api/export/pdf/{documentId} — 导出为 PDF 文档</li>
 * </ul>
 */
@Slf4j
@Tag(name = "导出管理", description = "译文导出（TXT/Markdown/DOCX/PDF）")
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    /**
     * 导出为 TXT 纯文本
     */
    @Operation(summary = "导出TXT", description = "将译文导出为纯文本文件")
    @GetMapping("/txt/{documentId}")
    public void exportTxt(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            HttpServletResponse response) {
        try {
            exportService.exportTxt(documentId, response);
        } catch (Exception e) {
            if (!response.isCommitted()) {
                throw e;
            }
            log.error("导出TXT失败(响应已提交): documentId={}", documentId, e);
        }
    }

    /**
     * 导出为 Markdown
     */
    @Operation(summary = "导出Markdown", description = "将译文导出为 Markdown 文件（含标题层级）")
    @GetMapping("/markdown/{documentId}")
    public void exportMarkdown(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            HttpServletResponse response) {
        try {
            exportService.exportMarkdown(documentId, response);
        } catch (Exception e) {
            if (!response.isCommitted()) {
                throw e;
            }
            log.error("导出Markdown失败(响应已提交): documentId={}", documentId, e);
        }
    }

    /**
     * 导出为 DOCX（Word 文档）
     */
    @Operation(summary = "导出DOCX", description = "将译文导出为 Word 文档（含格式化标题和正文）")
    @GetMapping("/docx/{documentId}")
    public void exportDocx(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            HttpServletResponse response) {
        try {
            exportService.exportDocx(documentId, response);
        } catch (Exception e) {
            if (!response.isCommitted()) {
                throw e;
            }
            log.error("导出DOCX失败(响应已提交): documentId={}", documentId, e);
        }
    }

    /**
     * 导出为 PDF
     */
    @Operation(summary = "导出PDF", description = "将译文导出为 PDF 文档（支持中文）")
    @GetMapping("/pdf/{documentId}")
    public void exportPdf(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            HttpServletResponse response) {
        try {
            exportService.exportPdf(documentId, response);
        } catch (Exception e) {
            if (!response.isCommitted()) {
                throw e;
            }
            log.error("导出PDF失败(响应已提交): documentId={}", documentId, e);
        }
    }
}