DROP TABLE IF EXISTS `translation_result`;

-- 翻译结果表
CREATE TABLE `translation_result` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `document_id` BIGINT       NOT NULL                COMMENT '所属文档ID',
    `chunk_id`    BIGINT       NOT NULL                COMMENT '所属分块ID',
    `source_text` TEXT         NOT NULL                COMMENT '原文',
    `target_text` TEXT                  DEFAULT NULL   COMMENT '译文',
    `source_lang` VARCHAR(20)  NOT NULL DEFAULT 'auto' COMMENT '源语言',
    `target_lang` VARCHAR(20)  NOT NULL DEFAULT 'zh'   COMMENT '目标语言',
    `style`       VARCHAR(20)           DEFAULT 'standard' COMMENT '翻译风格(standard/academic/business/technical)',
    `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0-待处理 1-处理中 2-已完成 3-失败',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_chunk_id` (`chunk_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='翻译结果表';
