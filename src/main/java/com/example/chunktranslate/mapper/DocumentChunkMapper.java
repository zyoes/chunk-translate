package com.example.chunktranslate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chunktranslate.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档分块表 Mapper，对应 document_chunk 表。
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {
}
