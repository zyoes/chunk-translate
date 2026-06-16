package com.example.chunktranslate.service.document.parser;

import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.dto.DocumentTreeNode;
import com.example.chunktranslate.util.ParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PowerPoint (.pptx) 文档解析器
 * <p>
 * 使用 Apache POI 解析 PPT 文件，每张幻灯片生成一个平级（level=1）节点。
 * </p>
 *
 * <p>核心流程：</p>
 * <ol>
 *   <li>通过 {@link XMLSlideShow} 加载 .pptx 文件</li>
 *   <li>遍历所有 {@link XSLFSlide}，提取每个形状（{@link XSLFTextShape}）中的文本</li>
 *   <li>自动识别幻灯片标题：第一个非空且长度 &lt;80 的文本块作为标题</li>
 * </ol>
 *
 * <p>PPT 无层级概念，所有节点均为平级。</p>
 *
 * @see DocumentParser
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PptxParserStrategy implements DocumentParser {

    /** 公共解析工具：截断摘要、估算 Token 数 */
    private final ParserUtil parserUtil;

    /**
     * 解析 PPT 文件，每张幻灯片生成一个章节节点
     *
     * @param filePath .pptx 文件路径
     * @return 幻灯片节点列表（平级，level=1）
     */
    @Override
    public List<DocumentTreeNode> parse(Path filePath) {
        List<DocumentTreeNode> nodes = new ArrayList<>();

        // XMLSlideShow：POI 表示 .pptx 文件的核心类
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XMLSlideShow pptx = new XMLSlideShow(fis)) {

            List<XSLFSlide> slides = pptx.getSlides();
            int index = 1;

            for (XSLFSlide slide : slides) {
                StringBuilder content = new StringBuilder();
                // 默认标题，后续会被幻灯片中第一个短文本覆盖
                String slideTitle = "幻灯片 " + index;

                // 遍历幻灯片中的所有形状（文本框、图片占位符等）
                for (XSLFShape shape : slide.getShapes()) {
                    // 只处理文本形状，忽略图片、图表等非文本元素
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (!text.isEmpty()) {
                            // 第一个短文本（<80字符）视为该幻灯片的标题
                            if (content.isEmpty() && text.length() < 80) {
                                slideTitle = text;
                            }
                            // 只有非空文本才追加到内容缓冲区
                            content.append(text).append("\n");
                        }
                    }
                }

                // 去除内容前后的空白
                String fullText = content.toString().trim();
                if (fullText.isEmpty()) continue;

                DocumentTreeNode node = new DocumentTreeNode();
                node.setNodeId(UUID.randomUUID().toString());
                node.setTitle(slideTitle);        // 幻灯片标题（自动识别或默认命名）
                node.setLevel(1);                 // PPT 不做层级嵌套
                node.setContent(fullText);
                node.setSummary(parserUtil.truncate(fullText, 100));
                node.setTokenCount(parserUtil.estimateTokens(fullText));
                node.setChildren(new ArrayList<>());
                nodes.add(node);
                index++;
            }

        } catch (IOException e) {
            log.error("PPTX 解析失败：{}", filePath, e);
            throw new BusinessException(ResultCode.DOCUMENT_PARSE_FAIL);
        }

        return nodes;
    }

    @Override
    public String supportedType() {
        return "pptx";
    }
}
