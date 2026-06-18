package com.example.chunktranslate.service.export;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 译文导出服务接口
 * <p>
 * 支持将翻译完成的文档导出为多种格式，通过 HTTP 响应流触发浏览器下载。
 * </p>
 *
 * <p>支持的导出格式：</p>
 * <ul>
 *   <li>TXT — 纯文本，各章节用换行分隔</li>
 *   <li>Markdown — 带标题层级（# 文档标题，## 章节标题）</li>
 *   <li>DOCX — Word 文档（Apache POI 生成，含格式化标题和正文）</li>
 *   <li>PDF — PDF 文档（iTextPDF 生成，支持中文）</li>
 * </ul>
 *
 * @see com.example.chunktranslate.service.export.impl.ExportServiceImpl
 */
public interface ExportService {

    /**
     * 导出为 TXT 纯文本
     *
     * @param documentId 文档ID
     * @param response   HTTP 响应（写入输出流触发下载）
     */
    void exportTxt(Long documentId, HttpServletResponse response);

    /**
     * 导出为 Markdown
     *
     * @param documentId 文档ID
     * @param response   HTTP 响应
     */
    void exportMarkdown(Long documentId, HttpServletResponse response);

    /**
     * 导出为 DOCX（Word）
     *
     * @param documentId 文档ID
     * @param response   HTTP 响应
     */
    void exportDocx(Long documentId, HttpServletResponse response);

    /**
     * 导出为 PDF
     *
     * @param documentId 文档ID
     * @param response   HTTP 响应
     */
    void exportPdf(Long documentId, HttpServletResponse response);

}
