-- Flyway V2：把用户/管理员密码列扩到 VARCHAR(100)，以容纳 BCrypt 哈希。
-- 仅改列宽，不迁移已有明文或旧哈希数据。

ALTER TABLE t_user
    MODIFY COLUMN password VARCHAR(100) NOT NULL,
    MODIFY COLUMN salt VARCHAR(32) NULL;

ALTER TABLE t_admin
    MODIFY COLUMN password VARCHAR(100) NOT NULL;
