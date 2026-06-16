package com.example.chunktranslate.service.document.parser;

import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 解析器策略工厂（策略模式 — Factory + Strategy）
 * <p>
 * 利用 Spring 的自动收集注入机制，将所有 {@link DocumentParser} 实现类
 * 按 {@link DocumentParser#supportedType()} 汇总为一张路由表（Map）。
 * </p>
 *
 * <p>工作原理：</p>
 * <ol>
 *   <li>Spring 启动时，构造器参数 {@code List<DocumentParser>} 自动收集所有标注了 {@code @Component} 的实现类</li>
 *   <li>通过 {@code Collectors.toMap} 构建 {@code Map<String, DocumentParser>} 路由表</li>
 *   <li>调用 {@link #getParser(String)} 时直接 O(1) 查表，找不到则抛出 {@link BusinessException}</li>
 * </ol>
 *
 * <p>扩展方式：新增文件格式只需新建一个实现类（如 {@code ExcelParserStrategy}），工厂代码无需修改。
 * 符合「开闭原则」（Open-Closed Principle）。</p>
 *
 * @see DocumentParser
 */
@Component
public class ParserStrategyFactory {

    /** 文件类型 → 解析器实例 的路由映射表（在构造器中一次性构建） */
    private final Map<String, DocumentParser> parserMap;

    /**
     * 构造器：Spring 自动注入所有 DocumentParser 实现类
     *
     * @param parsers 所有标注了 @Component 的解析器 Bean 列表
     */
    public ParserStrategyFactory(List<DocumentParser> parsers) {
        this.parserMap = parsers.stream()
                .collect(Collectors.toMap(DocumentParser::supportedType, Function.identity()));
    }

    /**
     * 根据文件类型获取对应的解析器
     *
     * @param fileType 文件类型（如 "pdf", "docx", "md"），不区分大小写
     * @return 对应的解析器实例
     * @throws BusinessException 当文件类型不被支持时抛出 {@link ResultCode#FILE_TYPE_NOT_SUPPORT}
     */
    public DocumentParser getParser(String fileType) {
        DocumentParser parser = parserMap.get(fileType.toLowerCase());

        if (parser == null) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORT);
        }

        return parser;
    }
}
