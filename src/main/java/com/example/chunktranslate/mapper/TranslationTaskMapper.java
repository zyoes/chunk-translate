package com.example.chunktranslate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chunktranslate.entity.TranslationTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 翻译任务表 Mapper，对应 translation_task 表。
 */
@Mapper
public interface TranslationTaskMapper extends BaseMapper<TranslationTask> {
}
