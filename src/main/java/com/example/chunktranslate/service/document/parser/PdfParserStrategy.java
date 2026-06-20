package com.example.chunktranslate.service.document.parser;

import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.dto.document.DocumentTreeNode;
import com.example.chunktranslate.util.ParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PDF 文档解析器
 * <p>
 * 使用 Apache PDFBox 3.x 提取 PDF 文件中的文本内容。
 * 采用「按页切分」策略：每一页生成一个平级（level=1）的 DocumentTreeNode。
 * </p>
 *
 * <p>核心流程：</p>
 * <ol>
 *   <li>通过 {@link Loader#loadPDF} 加载 PDF 文件到内存</li>
 *   <li>利用 {@link PDFTextStripper} 逐页提取文本</li>
 *   <li>将每页文本封装为独立的 {@link DocumentTreeNode}</li>
 * </ol>
 *
 * <p>注意：PDFBox 页码从 1 开始计数（1-based），而非 0-based。</p>
 *
 * @see DocumentParser
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfParserStrategy implements DocumentParser {

    /** 公共解析工具：截断摘要、估算 Token 数 */
    private final ParserUtil parserUtil;

    /**
     * 解析文档
     *
     * @param filePath 文件路径
     * @return 文档节点列表
     */
    @Override
    public List<DocumentTreeNode> parse(Path filePath) {
        List<DocumentTreeNode> nodes = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            // 遍历每一页
            for (int i = 0; i < totalPages; i++) {
                // 设置开始页和结束页
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document).trim();

                // 如果页面为空则跳过
                if (pageText.isEmpty()) continue;

                // 创建一个节点对象
                DocumentTreeNode node = new DocumentTreeNode();
                node.setNodeId(UUID.randomUUID().toString());
                node.setTitle("第 " + i + " 页");
                node.setLevel(1);
                node.setContent(pageText);
                node.setSummary(parserUtil.truncate(pageText, 100));
                node.setTokenCount(parserUtil.estimateTokens(pageText));
                nodes.add(node);
            }
        } catch (IOException e) {
            log.error("PDF 解析失败：{}", filePath, e);
            throw new BusinessException(ResultCode.DOCUMENT_PARSE_FAIL);
        }

        return nodes;
    }

    /**
     * 获取支持的文档类型
     *
     * @return 文档类型
     */
    @Override
    public String supportedType() {
        return "pdf";
    }
}
