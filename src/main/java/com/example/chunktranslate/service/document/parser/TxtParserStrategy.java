package com.example.chunktranslate.service.document.parser;

import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import com.example.chunktranslate.dto.document.DocumentTreeNode;
import com.example.chunktranslate.util.ParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 纯文本 (.txt) 文档解析器
 * <p>
 * 使用 JDK 原生 {@link Files#readString} 读取纯文本文件，
 * 按「空行」分段，每段生成一个平级（level=1）节点。
 * </p>
 *
 * <p>标题识别规则：取每段的第一行作为标题（若 &lt;80 字符），否则自动命名为「段落 N」。</p>
 *
 * <p>适用场景：没有结构标记的纯文本文件。对于有 Markdown 格式的文件请使用 {@link MarkdownParserStrategy}。</p>
 *
 * @see DocumentParser
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TxtParserStrategy implements DocumentParser {

    /** 公共解析工具：截断摘要、估算 Token 数 */
    private final ParserUtil parserUtil;

    /**
     * 解析纯文本文件，按空行分段
     *
     * @param filePath .txt 文件路径
     * @return 段落节点列表（平级，level=1）
     */
    @Override
    public List<DocumentTreeNode> parse(Path filePath) {
        List<DocumentTreeNode> nodes = new ArrayList<>();

        try {
            // 一次性读全文，适合中小型文件（MVP 阶段暂不考虑大文件流式处理）
            String fullText = Files.readString(filePath, StandardCharsets.UTF_8);

            // 按「空行」切分段落：正则 \n\s*\n 匹配连续空行（包括只有空格的行）
            String[] sections = fullText.split("\\n\\s*\\n");

            int index = 1;
            for (String section : sections) {
                String trimmed = section.trim();
                if (trimmed.isEmpty()) continue;

                // 提取标题：取段落首行，若过长则用「段落 N」代替
                String title = extractTitle(section, index);

                DocumentTreeNode node = new DocumentTreeNode();
                node.setNodeId(UUID.randomUUID().toString());
                node.setTitle(title);
                node.setLevel(1);           // 纯文本无层级，所有段落平级
                node.setContent(trimmed);
                node.setSummary(parserUtil.truncate(trimmed, 100));
                node.setTokenCount(parserUtil.estimateTokens(trimmed));
                node.setChildren(new ArrayList<>());
                nodes.add(node);
                index++;
            }

        } catch (IOException e) {
            log.error("Txt 解析失败：{}", filePath, e);
            throw new BusinessException(ResultCode.DOCUMENT_PARSE_FAIL);
        }

        return nodes;
    }

    @Override
    public String supportedType() {
        return "txt";
    }

    /**
     * 从段落中提取标题
     * <p>取段落第一行，若长度 &lt;80 字符则直接作为标题；否则自动命名为「段落 N」</p>
     *
     * @param text  段落原文
     * @param index 段落序号（用于兜底命名）
     * @return 标题文本
     */
    private String extractTitle(String text, int index) {
        String firstLine = text.split("\\n")[0].trim();
        if (firstLine.length() < 80) {
            return firstLine;
        }
        return "段落 " + index;
    }
}
