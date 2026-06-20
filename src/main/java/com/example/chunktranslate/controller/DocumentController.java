package com.example.chunktranslate.controller;

import com.example.chunktranslate.common.result.Result;
import com.example.chunktranslate.dto.document.DocumentDetailResponse;
import com.example.chunktranslate.dto.document.DocumentUploadResponse;
import com.example.chunktranslate.service.document.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文档管理", description = "文档管理相关接口")
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 上传文档
     *
     * @param file
     * @return 上传结果
     */
    @Operation(summary = "上传文档", description = "支持 PDF/DOCX/PPTX/TXT/MD，最大 100MB")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<DocumentUploadResponse> upload(
            @Parameter(description = "文档文件", required = true)
            @RequestParam("file") MultipartFile file) {
        return Result.success(documentService.uploadAndParse(file));
    }

    /**
     * 获取文档详情
     *
     * @param id
     * @return 文档详情
     */
    @Operation(summary = "获取文档详情", description = "返回文档基本信息 + 目录树")
    @GetMapping("/detail/{id}")
    public Result<DocumentDetailResponse> getDetail(@Parameter(description = "文档ID") @PathVariable Long id) {
        return Result.success(documentService.getDocumentDetail(id));
    }

}
