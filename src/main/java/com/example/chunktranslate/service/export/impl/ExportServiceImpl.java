package com.example.chunktranslate.service.export.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.chunktranslate.common.enums.ChunkStatus;
import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.entity.Document;
import com.example.chunktranslate.entity.DocumentChunk;
import com.example.chunktranslate.mapper.DocumentChunkMapper;
import com.example.chunktranslate.mapper.DocumentMapper;
import com.example.chunktranslate.service.export.ExportService;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 译文导出服务实现类
 * <p>
 * 实现 TXT、Markdown、DOCX、PDF 四种格式的译文导出。
 * 所有导出方法均通过 {@link HttpServletResponse} 的输出流直接写入，
 * 触发浏览器文件下载。
 * </p>
 *
 * <p>导出流程（通用）：</p>
 * <ol>
 *   <li>校验文档是否存在</li>
 *   <li>查询所有已翻译完成的 chunk（状态 = COMPLETED，按序号排序）</li>
 *   <li>根据目标格式组装内容并写入响应流</li>
 *   <li>设置 Content-Disposition 响应头触发浏览器下载</li>
 * </ol>
 *
 * <p>技术依赖：</p>
 * <ul>
 *   <li>TXT / Markdown — JDK 原生 IO</li>
 *   <li>DOCX — Apache POI {@link XWPFDocument}</li>
 *   <li>PDF — iTextPDF 5.x（{@link com.itextpdf.text.Document} + {@link PdfWriter}）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;

    private static final String FONT_RESOURCE = "fonts/NotoSansCJKsc-Regular.otf";

    /**
     * 导出为 TXT 纯文本
     * <p>
     * 将所有已翻译 chunk 的译文用换行符拼接，无任何格式化标记。
     * </p>
     */
    @Override
    public void exportTxt(Long documentId, HttpServletResponse response) {
        // 1. 校验文档 + 获取已翻译的 chunk 列表
        Document document = getDocumentOrThrow(documentId);
        List<DocumentChunk> chunks = getTranslatedChunks(documentId);

        // 2. 用 Stream 将所有译文用换行符拼接
        String fullText = chunks.stream()
                .map(DocumentChunk::getTranslation)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        // 3. 构建下载文件名并写入响应流
        String fileName = buildFileName(document.getFileName(), ".txt");
        writeTextResponse(response, fileName, fullText);
    }

    /**
     * 导出为 Markdown
     * <p>
     * 使用 # 和 ## 标记标题层级，生成标准 Markdown 格式文件。
     * </p>
     */
    @Override
    public void exportMarkdown(Long documentId, HttpServletResponse response) {
        Document document = getDocumentOrThrow(documentId);
        List<DocumentChunk> chunks = getTranslatedChunks(documentId);

        // 构建 Markdown 内容：# 文档标题 + ## 章节标题 + 正文
        StringBuilder md = new StringBuilder();
        md.append("# ").append(document.getFileName()).append("\n\n");

        for (DocumentChunk chunk : chunks) {
            // 章节标题（## 二级标题）：优先使用翻译后的标题
            String displayTitle = getDisplayTitle(chunk);
            if (displayTitle != null && !displayTitle.isEmpty()) {
                md.append("## ").append(displayTitle).append("\n\n");
            }
            // 正文段落
            md.append(chunk.getTranslation()).append("\n\n");
        }

        String fileName = buildFileName(document.getFileName(), ".md");
        writeTextResponse(response, fileName, md.toString());
    }

    /**
     * 导出为 DOCX（Word）
     * <p>
     * 使用 Apache POI {@link XWPFDocument} 创建 Word 文档：
     * 文档标题居中加粗 18pt，章节标题加粗 14pt，正文 12pt。
     * </p>
     */
    @Override
    public void exportDocx(Long documentId, HttpServletResponse response) {
        Document document = getDocumentOrThrow(documentId);
        List<DocumentChunk> chunks = getTranslatedChunks(documentId);

        String fileName = buildFileName(document.getFileName(), ".docx");

        // XWPFDocument：POI 创建 .docx 文件的核心类，try-with-resources 自动关闭
        try (XWPFDocument doc = new XWPFDocument()) {
            // 文档标题：居中、加粗、18号字
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setText(document.getFileName());

            // 遍历 chunk，写入章节标题 + 正文（标题优先使用翻译后的标题）
            for (DocumentChunk chunk : chunks) {
                // 章节标题：加粗、14号字
                String displayTitle = getDisplayTitle(chunk);
                if (displayTitle != null && !displayTitle.isEmpty()) {
                    XWPFParagraph headerPara = doc.createParagraph();
                    XWPFRun headerRun = headerPara.createRun();
                    headerRun.setBold(true);
                    headerRun.setFontSize(14);
                    headerRun.setText(displayTitle);
                }

                // 正文段落：12号字
                XWPFParagraph contentPara = doc.createParagraph();
                XWPFRun contentRun = contentPara.createRun();
                contentRun.setFontSize(12);
                contentRun.setText(chunk.getTranslation());
            }

            // 设置下载响应头 + 将文档序列化写入输出流
            setDownloadHeaders(response, fileName,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            doc.write(response.getOutputStream());

        } catch (IOException e) {
            log.error("导出DOCX失败: fileName={}", fileName, e);
            throw new BusinessException(ResultCode.EXPORT_FAIL);
        }
    }

    /**
     * 导出为 PDF
     * <p>
     * 使用 iTextPDF 5.x 创建 PDF 文档。由于 iTextPDF 默认不支持中文，
     * 使用 Windows 系统自带的宋体（simsun.ttc）作为字体。
     * </p>
     *
     * <p>注意：{@link com.itextpdf.text.Document} 与 {@link Document}（实体类）同名，
     * 因此 PDF 文档对象使用全限定名以避免冲突。</p>
     */
    @Override
    public void exportPdf(Long documentId, HttpServletResponse response) {
        Document document = getDocumentOrThrow(documentId);
        List<DocumentChunk> chunks = getTranslatedChunks(documentId);

        String fileName = buildFileName(document.getFileName(), ".pdf");

        // com.itextpdf.text.Document：iTextPDF 创建 PDF 的核心类（用全限定名避免与 entity.Document 冲突）
        com.itextpdf.text.Document pdfDoc = new com.itextpdf.text.Document(PageSize.A4);

        try {
            // 先设置响应头（必须在 getOutputStream 之前）
            setDownloadHeaders(response, fileName, "application/pdf");
            // PdfWriter：将 PDF 文档内容写入响应输出流
            PdfWriter.getInstance(pdfDoc, response.getOutputStream());

            pdfDoc.open();

            // 从 classpath 加载中文字体（避免硬编码系统路径）
            byte[] fontBytes;
            try (InputStream fontStream = getClass().getClassLoader()
                    .getResourceAsStream(FONT_RESOURCE)) {
                if (fontStream == null) {
                    throw new BusinessException(ResultCode.EXPORT_FAIL.getCode(),
                            "中文字体文件未找到: " + FONT_RESOURCE);
                }
                fontBytes = fontStream.readAllBytes();
            }
            BaseFont baseFont = BaseFont.createFont(
                    "NotoSansCJKsc-Regular.otf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,    // 嵌入式，PDF 可在任何设备正常显示
                    false,
                    fontBytes,
                    null
            );

            Font titleFont = new Font(baseFont, 18, Font.BOLD);
            Font headingFont = new Font(baseFont, 14, Font.BOLD);
            Font contentFont = new Font(baseFont, 12);

            // 文档标题
            pdfDoc.add(new Paragraph(document.getFileName(), titleFont));
            pdfDoc.add(new Paragraph("")); // 空行分隔

            // 遍历 chunk，写入章节标题 + 正文（标题优先使用翻译后的标题）
            for (DocumentChunk chunk : chunks) {
                String displayTitle = getDisplayTitle(chunk);
                if (displayTitle != null && !displayTitle.isEmpty()) {
                    pdfDoc.add(new Paragraph(displayTitle, headingFont));
                }
                pdfDoc.add(new Paragraph(chunk.getTranslation(), contentFont));
                pdfDoc.add(new Paragraph("")); // 段落间空行
            }

        } catch (Exception e) {
            log.error("导出PDF失败: fileName={}", fileName, e);
            throw new BusinessException(ResultCode.EXPORT_FAIL);
        } finally {
            // finally 中关闭文档，确保资源释放
            pdfDoc.close();
        }
    }

    /**
     * 获取用于导出的显示标题
     * <p>
     * 优先使用翻译后的标题 {@code translatedTitle}，若为空则回退到原文标题 {@code title}。
     * 对于系统自动生成的合成标题（如"段落N - ..."、"幻灯片N"、"第N页"、"全文"等），
     * 返回 null 以在导出时跳过，使导出文件与原文一样只包含正文内容。
     * </p>
     */
    private String getDisplayTitle(DocumentChunk chunk) {
        // 原文标题是合成标题 → 跳过
        if (isSyntheticTitle(chunk.getTitle())) {
            return null;
        }
        // 优先使用翻译后的标题
        if (chunk.getTranslatedTitle() != null && !chunk.getTranslatedTitle().isBlank()) {
            return chunk.getTranslatedTitle();
        }
        return chunk.getTitle();
    }

    /**
     * 判断标题是否为系统自动生成的合成标题
     * <p>
     * 以下模式被视为合成标题：
     * </p>
     * <ul>
     *   <li>"段落N" / "段落N - ..."（DocxParserStrategy / TxtParserStrategy）</li>
     *   <li>"全文"（DocxParserStrategy / MarkdownParserStrategy）</li>
     *   <li>"幻灯片 N"（PptxParserStrategy）</li>
     *   <li>"第 N 页"（PdfParserStrategy）</li>
     * </ul>
     */
    private boolean isSyntheticTitle(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        return title.matches("段落\\s*\\d+.*")
                || title.equals("全文")
                || title.matches("幻灯片\\s*\\d+")
                || title.matches("第\\s*\\d+\\s*页");
    }

    /**
     * 查询文档，不存在则抛 {@link ResultCode#DOCUMENT_NOT_FOUND} 异常
     */
    private Document getDocumentOrThrow(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.DOCUMENT_NOT_FOUND);
        }
        return document;
    }

    /**
     * 查询已翻译完成的 chunk 列表（按序号升序）
     * <p>
     * 只查询状态为 {@link ChunkStatus#COMPLETED} 的 chunk，
     * 若结果为空则抛出 {@link ResultCode#EXPORT_FAIL}（说明翻译未完成）。
     * </p>
     */
    private List<DocumentChunk> getTranslatedChunks(Long documentId) {
        List<DocumentChunk> chunks = documentChunkMapper.selectList(
                new LambdaQueryWrapper<DocumentChunk>()
                        .eq(DocumentChunk::getDocumentId, documentId)
                        .eq(DocumentChunk::getStatus, ChunkStatus.COMPLETED.getCode())
                        .orderByAsc(DocumentChunk::getSequence)
        );
        if (chunks.isEmpty()) {
            throw new BusinessException(ResultCode.EXPORT_FAIL);
        }
        return chunks;
    }

    /**
     * 构建导出文件名
     * <p>
     * 将原始文件名去掉扩展名，拼接 {@code _translated} 后缀和新扩展名。
     * 例如：{@code report.docx → report_translated.pdf}
     * </p>
     */
    private String buildFileName(String originalName, String extension) {
        String baseName = originalName;
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        return baseName + "_translated" + extension;
    }

    /**
     * 写入纯文本响应（TXT / Markdown 共用）
     * <p>
     * 设置 Content-Type 为 text/plain，并将文本内容以 UTF-8 编码写入输出流。
     * </p>
     */
    private void writeTextResponse(HttpServletResponse response, String fileName, String content) {
        try {
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            response.setContentType("text/plain;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);

            OutputStream os = response.getOutputStream();
            os.write(content.getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            log.error("导出文件失败: fileName={}", fileName, e);
            throw new BusinessException(ResultCode.EXPORT_FAIL);
        }
    }

    /**
     * 设置 HTTP 下载响应头（DOCX / PDF 共用）
     * <p>
     * Content-Disposition: attachment 告诉浏览器触发文件下载，
     * filename*=UTF-8'' 支持中文文件名（RFC 5987 标准）。
     * </p>
     *
     * @param response    HTTP 响应
     * @param fileName    文件名（会被 URL 编码）
     * @param contentType MIME 类型
     */
    private void setDownloadHeaders(HttpServletResponse response, String fileName, String contentType) {
        try {
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        } catch (Exception e) {
            log.error("设置响应头失败", e);
        }
    }
}
