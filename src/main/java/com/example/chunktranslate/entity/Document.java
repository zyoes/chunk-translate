package com.example.chunktranslate.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.chunktranslate.common.entity.BaseEntity;
import lombok.Data;

/**
 * 文档实体，对应 document 表。
 * <p>记录上传文件的元数据信息，状态流转参考 {@link com.example.chunktranslate.common.enums.DocumentStatus}。</p>
 */
@Data
@TableName("document")
public class Document extends BaseEntity {
    /** 原始文件名 */
    private String fileName;

    /** 文件在服务器上的存储路径 */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件类型/扩展名 */
    private String fileType;

    /** 文档处理状态，对应 {@link com.example.chunktranslate.common.enums.DocumentStatus} */
    private Integer status;
}
