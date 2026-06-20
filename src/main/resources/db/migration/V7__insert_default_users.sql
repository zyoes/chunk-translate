-- ============================================================
-- V7: 插入默认管理员和普通用户
-- ============================================================

-- 默认管理员: admin@example.com / admin123
INSERT INTO `sys_user` (`username`, `email`, `password`, `provider`, `role`, `status`, `created_at`, `updated_at`)
SELECT 'admin', 'admin@example.com', '$2a$10$nEJxotdaQxGIsBJuC9Un9uWa3cfZwc8kXpCp6FZgQddsbHPvl0gRK', 'local', 'admin', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'admin');

-- 默认普通用户: user@example.com / user123
INSERT INTO `sys_user` (`username`, `email`, `password`, `provider`, `role`, `status`, `created_at`, `updated_at`)
SELECT 'user', 'user@example.com', '$2a$10$AKadG2fpkHdK1V7osAF2YeOwJgRSA2N4qILYxJONRRw47vx1BgAgy', 'local', 'user', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'user');
