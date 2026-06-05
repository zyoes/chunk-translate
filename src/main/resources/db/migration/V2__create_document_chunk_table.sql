DROP TABLE IF EXISTS `document_chunk`;

-- 文档分块表
CREATE TABLE `document_chunk` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `document_id` BIGINT       NOT NULL                COMMENT '所属文档ID',
    `sequence`    INT          NOT NULL DEFAULT 0      COMMENT '分块序号(从1开始)',
    `title`       VARCHAR(255)          DEFAULT NULL   COMMENT '章节标题',
    `content`     TEXT         NOT NULL                COMMENT '分块原文内容',
    `token_count` INT          NOT NULL DEFAULT 0      COMMENT '预估Token数',
    `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0-待翻译 1-翻译中 2-已完成 3-翻译失败',
    `translation` TEXT                  DEFAULT NULL   COMMENT '翻译结果',
    `error_msg`   VARCHAR(512)          DEFAULT NULL   COMMENT '失败原因',
    `retry_count` INT          NOT NULL DEFAULT 0      COMMENT '重试次数',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_document_sequence` (`document_id`, `sequence`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档分块表';
