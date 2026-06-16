package com.example.chunktranslate.service.document.parser;

import com.example.chunktranslate.common.exception.BusinessException;
import com.example.chunktranslate.common.result.ResultCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ParserStrategyFactory {

    private final Map<String,DocumentParser> parserMap;

    public ParserStrategyFactory(List<DocumentParser> parsers) {
        this.parserMap = parsers.stream()
                .collect(Collectors.toMap(DocumentParser::supportedType, Function.identity()));
    }

    public DocumentParser getParser(String fileType){
        DocumentParser parser = parserMap.get(fileType.toLowerCase());

        if(parser == null){
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORT);
        }

        return parser;
    }
}
