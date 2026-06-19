-- ============================================================
-- V5: 刷新令牌表
-- ============================================================

DROP TABLE IF EXISTS `refresh_token`;
CREATE TABLE `refresh_token`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
    `token`      VARCHAR(512) NOT NULL COMMENT '刷新令牌',
    `expires_at` DATETIME     NOT NULL COMMENT '过期时间',
    `revoked`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已撤销: 0-否 1-是',

    `created_at` datetime              DEFAULT NULL COMMENT '创建时间',
    `created_by` bigint                DEFAULT NULL COMMENT '创建人ID',
    `updated_at` datetime              DEFAULT NULL COMMENT '更新时间',
    `updated_by` bigint                DEFAULT NULL COMMENT '修改人ID',
    `deleted`    bigint                DEFAULT 0 COMMENT '逻辑删除标记',
    `deleted_at` datetime              DEFAULT NULL COMMENT '删除时间',
    `deleted_by` bigint                DEFAULT NULL COMMENT '删除人ID',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token`(255)),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='刷新令牌表';