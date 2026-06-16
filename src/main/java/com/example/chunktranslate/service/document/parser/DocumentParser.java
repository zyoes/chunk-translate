package com.example.chunktranslate.service.document.parser;

import com.example.chunktranslate.dto.DocumentTreeNode;

import java.nio.file.Path;
import java.util.List;

/**
 * 文档解析器接口（策略模式 — Strategy Pattern）
 * <p>
 * 定义所有文档解析器的统一契约。每种文件格式（PDF/DOCX/PPTX/TXT/MD）
 * 各自实现该接口，并通过 {@link ParserStrategyFactory} 统一路由分发。
 * </p>
 *
 * <p>设计要点：</p>
 * <ul>
 *   <li>所有实现类必须标注 {@code @Component} 以便被 Spring 自动收集注入到工厂</li>
 *   <li>{@link #supportedType()} 返回值必须唯一且小写（如 "pdf", "docx"），工厂以此为 Key 建立路由表</li>
 *   <li>{@link #parse(Path)} 的返回值是一个树节点列表，支持多章节平级或多级嵌套</li>
 * </ul>
 *
 * @see ParserStrategyFactory
 */
public interface DocumentParser {

    /**
     * 解析文档，返回章节树节点列表
     * <p>
     * 返回值为 {@code List<DocumentTreeNode>} 以支持：
     * <ul>
     *   <li>多个平级根节点（如 PDF 按页切分、DOCX 有多个 H1）</li>
     *   <li>单个根节点（如全文无标题时）</li>
     * </ul>
     *
     * @param filePath 待解析文件在磁盘上的路径
     * @return 章节树根节点列表
     */
    List<DocumentTreeNode> parse(Path filePath);

    /**
     * 当前解析器支持的文件类型标识（小写，无点号）
     * <p>例如：{@code "pdf"}, {@code "docx"}, {@code "md"}</p>
     *
     * @return 文件类型标识字符串
     */
    String supportedType();

}
