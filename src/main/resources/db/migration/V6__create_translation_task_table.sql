-- ============================================================
-- V6: 翻译任务表（用户级翻译历史）
-- ============================================================

DROP TABLE IF EXISTS `translation_task`;
CREATE TABLE `translation_task`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT      NOT NULL COMMENT '用户ID',
    `document_id`     BIGINT      NOT NULL COMMENT '文档ID',
    `source_lang`     VARCHAR(20) NOT NULL COMMENT '源语言',
    `target_lang`     VARCHAR(20) NOT NULL COMMENT '目标语言',
    `status`          TINYINT     NOT NULL DEFAULT 0 COMMENT '状态: 0-进行中 1-已完成 2-失败',
    `total_chunks`    INT         NOT NULL DEFAULT 0 COMMENT '总分块数',
    `completed_chunks` INT        NOT NULL DEFAULT 0 COMMENT '已完成分块数',
    `started_at`      DATETIME             DEFAULT NULL COMMENT '开始时间',
    `completed_at`    DATETIME             DEFAULT NULL COMMENT '完成时间',

    `created_at`      datetime             DEFAULT NULL COMMENT '创建时间',
    `created_by`      bigint               DEFAULT NULL COMMENT '创建人ID',
    `updated_at`      datetime             DEFAULT NULL COMMENT '更新时间',
    `updated_by`      bigint               DEFAULT NULL COMMENT '修改人ID',
    `deleted`         bigint               DEFAULT 0 COMMENT '逻辑删除标记',
    `deleted_at`      datetime             DEFAULT NULL COMMENT '删除时间',
    `deleted_by`      bigint               DEFAULT NULL COMMENT '删除人ID',

    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='翻译任务表';