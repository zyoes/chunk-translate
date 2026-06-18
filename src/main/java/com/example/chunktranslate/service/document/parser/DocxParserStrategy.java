package com.example.chunktranslate.service.document.parser;

import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.dto.DocumentTreeNode;
import com.example.chunktranslate.util.ParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Word (.docx) 文档解析器
 * <p>
 * 使用 Apache POI 解析 Word 文档，支持多级标题嵌套树结构。
 * </p>
 *
 * <p>核心算法 — 「栈式标题层级构建」：</p>
 * <ol>
 *   <li>遍历文档中所有 {@link IBodyElement}，识别段落（{@link XWPFParagraph}）</li>
 *   <li>通过段落样式名（Heading1/Heading2/...）判断标题层级</li>
 *   <li>使用 {@link Deque}（栈）维护标题路径：遇到新标题时，弹出同级及更深的标题，新标题入栈</li>
 *   <li>非标题段落累积为当前节的文本内容，遇到新标题时 flush 写入上一节</li>
 * </ol>
 *
 * <p>特殊处理：若全文无标题样式，则整个文档作为一个单节点返回。</p>
 *
 * @see DocumentParser
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocxParserStrategy implements DocumentParser {

    /** 公共解析工具：截断摘要、估算 Token 数 */
    private final ParserUtil parserUtil;

    /**
     * 解析 Word 文档，构建多级标题树
     *
     * @param filePath .docx 文件路径
     * @return 章节树根节点列表（可能有多个 H1 同级标题）
     */
    @Override
    public List<DocumentTreeNode> parse(Path filePath) {
        // roots：树的顶层节点列表（所有 level=1 的标题）
        // stack：维护当前标题路径的栈，栈顶为最近一个标题节点
        // currentSection：当前正在累积内容的章节节点
        // sectionContent：当前章节下所有非标题段落的文本累积
        List<DocumentTreeNode> roots = new ArrayList<>();
        Deque<DocumentTreeNode> stack = new ArrayDeque<>();
        DocumentTreeNode currentSection = null;
        StringBuilder sectionContent = new StringBuilder();

        // XWPFDocument：POI 表示 .docx 文档的核心类
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             XWPFDocument document = new XWPFDocument(fis)) {

            // getBodyElements() 返回文档中的所有块级元素（段落 + 表格等）
            for (IBodyElement element : document.getBodyElements()) {
                // 只处理段落，表格等其他元素暂时忽略
                if (element instanceof XWPFParagraph paragraph) {
                    String style = paragraph.getStyle();   // 段落样式名，如 "Heading1", "Normal"
                    String text = paragraph.getText();

                    if (text.isEmpty()) continue;

                    // 判断当前段落是否为标题（Heading1/Heading2/...）
                    int headingLevel = getHeadingLevel(style);

                    if (headingLevel > 0) {
                        // 遇到新标题：先把上一节累积的内容写入上一节节点
                        if (currentSection != null) {
                            flushSection(currentSection, sectionContent);
                        }

                        // 为新标题创建节点
                        DocumentTreeNode node = new DocumentTreeNode();
                        node.setNodeId(UUID.randomUUID().toString());
                        node.setTitle(text);
                        node.setLevel(headingLevel);
                        node.setChildren(new ArrayList<>());

                        // 通过栈将节点插入到正确的父级下（核心算法）
                        insertNode(roots, stack, node, headingLevel);
                        currentSection = node;
                        sectionContent = new StringBuilder();
                    } else {
                        // 普通段落：追加到当前节的内容缓冲区
                        sectionContent.append(text).append("\n");
                    }
                }
            }

            // 文档遍历完毕后，写入最后一节的内容
            if (currentSection != null) {
                flushSection(currentSection, sectionContent);
            }

            // 全文无标题样式时，按段落智能分块（每约1500 tokens 或 20段为一块）
            if (roots.isEmpty()) {
                // 收集所有非空段落
                List<String> paragraphs = new ArrayList<>();
                for (XWPFParagraph paragraph : document.getParagraphs()) {
                    String text = paragraph.getText().trim();
                    if (!text.isEmpty()) {
                        paragraphs.add(text);
                    }
                }

                if (paragraphs.isEmpty()) {
                    // 文档完全为空，创建一个空节点
                    DocumentTreeNode node = new DocumentTreeNode();
                    node.setNodeId(UUID.randomUUID().toString());
                    node.setTitle("全文");
                    node.setLevel(1);
                    node.setContent("");
                    node.setChildren(new ArrayList<>());
                    node.setTokenCount(0);
                    roots.add(node);
                } else {
                    // 按段落累积分块
                    final int TARGET_TOKENS = 1500;   // 每块目标 token 数
                    final int MAX_PARAGRAPHS = 20;     // 每块最大段落数

                    int chunkIndex = 1;
                    StringBuilder chunkContent = new StringBuilder();
                    int chunkTokens = 0;
                    int chunkParaCount = 0;
                    String chunkFirstSentence = null;  // 块内第一句话作为标题

                    for (String para : paragraphs) {
                        int paraTokens = parserUtil.estimateTokens(para);

                        // 当前块已有内容，且加上新段落会超限 → 先封存当前块
                        if (chunkParaCount > 0
                                && (chunkTokens + paraTokens > TARGET_TOKENS
                                    || chunkParaCount >= MAX_PARAGRAPHS)) {
                            // 封存当前块
                            DocumentTreeNode node = new DocumentTreeNode();
                            node.setNodeId(UUID.randomUUID().toString());
                            node.setTitle(buildChunkTitle(chunkFirstSentence, chunkIndex));
                            node.setLevel(1);
                            node.setContent(chunkContent.toString().trim());
                            node.setChildren(new ArrayList<>());
                            node.setSummary(parserUtil.truncate(chunkContent.toString().trim(), 100));
                            node.setTokenCount(chunkTokens);
                            roots.add(node);

                            chunkIndex++;
                            chunkContent = new StringBuilder();
                            chunkTokens = 0;
                            chunkParaCount = 0;
                            chunkFirstSentence = null;
                        }

                        // 将段落加入当前块
                        if (chunkFirstSentence == null) {
                            chunkFirstSentence = para;
                        }
                        chunkContent.append(para).append("\n");
                        chunkTokens += paraTokens;
                        chunkParaCount++;
                    }

                    // 封存最后一块
                    if (chunkParaCount > 0) {
                        DocumentTreeNode node = new DocumentTreeNode();
                        node.setNodeId(UUID.randomUUID().toString());
                        node.setTitle(buildChunkTitle(chunkFirstSentence, chunkIndex));
                        node.setLevel(1);
                        node.setContent(chunkContent.toString().trim());
                        node.setChildren(new ArrayList<>());
                        node.setSummary(parserUtil.truncate(chunkContent.toString().trim(), 100));
                        node.setTokenCount(chunkTokens);
                        roots.add(node);
                    }
                }
            }


        } catch (IOException e) {
            log.error("DOCX 解析失败：{}", filePath, e);
            throw new BusinessException(ResultCode.DOCUMENT_PARSE_FAIL);
        }

        return roots;
    }

    /**
     * 获取支持的文件类型
     */
    @Override
    public String supportedType() {
        return "docx";
    }

    /**
     * 从段落样式名提取标题级别
     * Heading1 → 1, Heading2 → 2, 中文"标题1"也兼容
     */
    private int getHeadingLevel(String style) {
        if (style == null) return 0;
        if (style.startsWith("Heading") || style.startsWith("heading") || style.contains("标题")) {
            try {
                return Integer.parseInt(style.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 用栈维护层级关系，将节点插入到正确的父级下
     */
    private void insertNode(List<DocumentTreeNode> roots, Deque<DocumentTreeNode> stack,
                            DocumentTreeNode node, int level) {
        while (!stack.isEmpty() && stack.peek().getLevel() >= level) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            roots.add(node);
        } else {
            stack.peek().getChildren().add(node);
        }
        stack.push(node);
    }

    /**
     * 保存节内容并重置
     */
    private void flushSection(DocumentTreeNode node, StringBuilder content) {
        String text = content.toString().trim();
        node.setContent(text);
        node.setSummary(parserUtil.truncate(text, 100));
        node.setTokenCount(parserUtil.estimateTokens(text));
    }

    /**
     * 为无标题分块生成标题
     * <p>
     * 取块内第一段的前 50 个字符作为标题，加上序号前缀。
     * 例："段落1 - 这是第一章的内容介绍..."
     * </p>
     *
     * @param firstParagraph 块内第一段文本
     * @param chunkIndex     块序号（从 1 开始）
     * @return 生成的标题
     */
    private String buildChunkTitle(String firstParagraph, int chunkIndex) {
        if (firstParagraph == null || firstParagraph.isEmpty()) {
            return "段落 " + chunkIndex;
        }
        // 取第一句话（以句号/感叹号/问号分割），或截取前 50 字符
        String title = firstParagraph.split("[。！？.!?]")[0].trim();
        if (title.length() > 50) {
            title = title.substring(0, 50) + "...";
        }
        return "段落" + chunkIndex + " - " + title;
    }
}
