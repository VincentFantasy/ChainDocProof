-- =============================================================
-- 链上资产存证服务：管理端账户初始化脚本（MySQL 8.x）
-- 默认账户: admin / admin123
-- 注意: 上线前务必修改密码，生成新 BCrypt 哈希后执行:
--   UPDATE t_admin SET password_hash = '新哈希' WHERE username = 'admin';
-- =============================================================
USE chaindocproof;

INSERT INTO t_admin (username, password_hash)
VALUES ('admin', '$2a$10$/UVQj9AM7Xj80FfAFOzX5.D72UbMKSr5EPO0dQJvS4Iy.ElvVDo8S')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);
