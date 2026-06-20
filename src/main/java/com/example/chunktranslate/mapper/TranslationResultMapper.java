package com.example.chunktranslate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chunktranslate.entity.TranslationResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 翻译结果表 Mapper，对应 translation_result 表。
 */
@Mapper
public interface TranslationResultMapper extends BaseMapper<TranslationResult> {
}
