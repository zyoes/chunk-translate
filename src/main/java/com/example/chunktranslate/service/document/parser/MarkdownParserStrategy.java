package com.example.chunktranslate.service.document.parser;

import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.dto.DocumentTreeNode;
import com.example.chunktranslate.util.ParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown (.md) 文档解析器
 * <p>
 * 通过正则表达式识别 Markdown 标题语法（# ~ ######），
 * 构建与 {@link DocxParserStrategy} 相同的多级嵌套树结构。
 * </p>
 *
 * <p>核心算法 — 与 DOCX 解析器相同的「栈式标题层级构建」：</p>
 * <ol>
 *   <li>逐行扫描文件内容</li>
 *   <li>匹配标题正则 {@code ^(#{1,6})\s+(.+)$}，通过 # 的个数确定层级（1~6）</li>
 *   <li>使用栈维护标题路径，将新节点插入正确的父级下</li>
 *   <li>非标题行累积为当前节的文本内容</li>
 * </ol>
 *
 * <p>特殊处理：若全文无 Markdown 标题，则整个文件作为一个单节点返回。</p>
 *
 * @see DocumentParser
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarkdownParserStrategy implements DocumentParser {

    /** 公共解析工具：截断摘要、估算 Token 数 */
    private final ParserUtil parserUtil;

    /**
     * Markdown 标题匹配正则
     * <p>
     * 匹配 {@code # Title} 到 {@code ###### Title} 六种层级的标题。
     * group(1) = # 序列（用于计算层级），group(2) = 标题文本。
     * </p>
     */
    private static final Pattern HEADING_PATTERN =
            Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    /**
     * 解析 Markdown 文件，通过 # 标题构建多级嵌套树
     *
     * @param filePath .md 文件路径
     * @return 章节树根节点列表
     */
    @Override
    public List<DocumentTreeNode> parse(Path filePath) {
        List<DocumentTreeNode> roots = new ArrayList<>();
        Deque<DocumentTreeNode> stack = new ArrayDeque<>();
        DocumentTreeNode currentSection = null;
        StringBuilder sectionContent = new StringBuilder();

        try {
            String fullText = Files.readString(filePath, StandardCharsets.UTF_8);
            // 按行切分，逐行扫描判断是否为标题
            String[] lines = fullText.split("\n");

            for (String line : lines) {
                Matcher matcher = HEADING_PATTERN.matcher(line);

                if (matcher.find()) {
                    // 匹配到标题行：先把上一节累积的内容写入上一节节点
                    if (currentSection != null) {
                        flushSection(currentSection, sectionContent);
                    }

                    // group(1) = "##"，length() 即为层级数（2）
                    // group(2) = "标题文本"
                    int level = matcher.group(1).length();
                    String title = matcher.group(2).trim();

                    DocumentTreeNode node = new DocumentTreeNode();
                    node.setNodeId(UUID.randomUUID().toString());
                    node.setTitle(title);
                    node.setLevel(level);
                    node.setChildren(new ArrayList<>());

                    // 栈式层级构建（与 DOCX 解析器逻辑相同）：
                    // 弹出栈中所有层级 >= 当前层级的节点，新节点挂在栈顶节点下面
                    while (!stack.isEmpty() && stack.peek().getLevel() >= level) {
                        stack.pop();
                    }

                    if (stack.isEmpty()) {
                        // 栈为空说明是顶级标题（或更高层级），加入 roots
                        roots.add(node);
                    } else {
                        // 挂在栈顶（父级）的 children 下
                        stack.peek().getChildren().add(node);
                    }
                    stack.push(node);

                    currentSection = node;
                    sectionContent = new StringBuilder();
                } else {
                    // 非标题行：累积到当前节的文本缓冲区
                    sectionContent.append(line).append("\n");
                }
            }

            // 写入最后一节内容
            if (currentSection != null) {
                flushSection(currentSection, sectionContent);
            }

            // 全文无 # 标题时，将整个文件作为单节点返回
            if (roots.isEmpty()) {
                DocumentTreeNode node = new DocumentTreeNode();
                node.setNodeId(UUID.randomUUID().toString());
                node.setTitle("全文");
                node.setLevel(1);
                node.setContent(fullText);
                node.setSummary(parserUtil.truncate(fullText, 100));
                node.setTokenCount(parserUtil.estimateTokens(fullText));
                node.setChildren(new ArrayList<>());
                roots.add(node);
            }
        } catch (IOException e) {
            log.error("Markdown 解析失败：{}", filePath, e);
            throw new BusinessException(ResultCode.DOCUMENT_PARSE_FAIL);
        }

        return roots;
    }

    @Override
    public String supportedType() {
        return "md";
    }

    /**
     * 将累积的文本内容写入节点，并设置摘要和 Token 数估算
     *
     * @param node    目标章节节点
     * @param content 该章节下累积的文本内容
     */
    private void flushSection(DocumentTreeNode node, StringBuilder content) {
        String text = content.toString().trim();
        node.setContent(text);
        node.setSummary(parserUtil.truncate(text, 100));
        node.setTokenCount(parserUtil.estimateTokens(text));
    }

}
