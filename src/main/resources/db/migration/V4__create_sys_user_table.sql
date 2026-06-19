-- ============================================================
-- V4: 系统用户表
-- ============================================================

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`      VARCHAR(50)  NOT NULL COMMENT '用户名',
    `email`         VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    `password`      VARCHAR(255)          DEFAULT NULL COMMENT '密码（bcrypt）',
    `provider`      VARCHAR(20)  NOT NULL DEFAULT 'local' COMMENT '认证提供者: local / github',
    `provider_id`   VARCHAR(100)          DEFAULT NULL COMMENT '第三方用户ID',
    `avatar_url`    VARCHAR(512)          DEFAULT NULL COMMENT '头像 URL',
    `role`          VARCHAR(20)  NOT NULL DEFAULT 'user' COMMENT '角色: admin / user',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `last_login_at` DATETIME             DEFAULT NULL COMMENT '最后登录时间',

    `created_at`    datetime              DEFAULT NULL COMMENT '创建时间',
    `created_by`    bigint                DEFAULT NULL COMMENT '创建人ID',
    `updated_at`    datetime              DEFAULT NULL COMMENT '更新时间',
    `updated_by`    bigint                DEFAULT NULL COMMENT '修改人ID',
    `deleted`       bigint                DEFAULT 0 COMMENT '逻辑删除标记',
    `deleted_at`    datetime              DEFAULT NULL COMMENT '删除时间',
    `deleted_by`    bigint                DEFAULT NULL COMMENT '删除人ID',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_email` (`email`),
    KEY `idx_provider_provider_id` (`provider`, `provider_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';