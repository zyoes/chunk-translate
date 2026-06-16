DROP TABLE IF EXISTS `document_chunk`;

-- 文档分块表
CREATE TABLE `document_chunk`
(
    `id`          BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `document_id` BIGINT  NOT NULL COMMENT '所属文档ID',
    `sequence`    INT     NOT NULL DEFAULT 0 COMMENT '分块序号(从1开始)',
    `title`       VARCHAR(255)     DEFAULT NULL COMMENT '章节标题',
    `content`     TEXT    NOT NULL COMMENT '分块原文内容',
    `token_count` INT     NOT NULL DEFAULT 0 COMMENT '预估Token数',
    `status`      TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待翻译 1-翻译中 2-已完成 3-翻译失败',
    `translation` TEXT             DEFAULT NULL COMMENT '翻译结果',
    `error_msg`   VARCHAR(512)     DEFAULT NULL COMMENT '失败原因',
    `retry_count` INT     NOT NULL DEFAULT 0 COMMENT '重试次数',

    -- 通用基础字段
    `created_at`  datetime         default null comment '创建时间',
    `created_by`  bigint           default null comment '创建人ID',
    `updated_at`  datetime         default null comment '更新时间',
    `updated_by`  bigint           default null comment '修改人ID',
    `deleted`     bigint           default 0 comment '逻辑删除标记',
    `deleted_at`  datetime         default null comment '删除时间',
    `deleted_by`  bigint           default null comment '删除人ID',

    PRIMARY KEY (`id`),
    KEY           `idx_document_id` (`document_id`),
    KEY           `idx_document_sequence` (`document_id`, `sequence`),
    KEY           `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档分块表';
