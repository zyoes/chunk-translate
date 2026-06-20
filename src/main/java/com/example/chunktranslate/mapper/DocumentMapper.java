package com.example.chunktranslate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chunktranslate.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 文档表 Mapper，对应 document 表。
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    /**
     * 按 ID 列表批量查询文档（绕过逻辑删除过滤，包含已删除的文档）。
     * 用于后台翻译任务列表关联显示文档名。
     */
    @Select("<script>" +
            "SELECT * FROM document WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            " ORDER BY created_at DESC" +
            "</script>")
    List<Document> selectByIdsIncludeDeleted(@Param("ids") Collection<Long> ids);
}
