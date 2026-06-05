DROP TABLE IF EXISTS `document`;

-- 文档表
CREATE TABLE `document` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_name`   VARCHAR(255) NOT NULL                COMMENT '原始文件名',
    `file_path`   VARCHAR(512) NOT NULL                COMMENT '存储路径',
    `file_size`   BIGINT       NOT NULL DEFAULT 0      COMMENT '文件大小(字节)',
    `file_type`   VARCHAR(20)  NOT NULL                COMMENT '文件类型(pdf/docx/pptx/txt/md)',
    `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0-已上传 1-解析中 2-已解析 3-解析失败',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';
