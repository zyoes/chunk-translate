package com.example.chunktranslate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chunktranslate.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档表 Mapper，对应 document 表。
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
