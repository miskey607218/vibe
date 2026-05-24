-- 1. 给 tb_user 添加 user_type 字段
ALTER TABLE tb_user ADD COLUMN user_type TINYINT NOT NULL DEFAULT 0 COMMENT '用户类型：0-普通用户，1-管理员';

-- 2. 迁移 tb_admin 数据到 tb_user（用户名作为username，密码从tb_admin，邮箱自动生成）
INSERT INTO tb_user (username, password, email, create_time, update_time, status, user_type)
SELECT
    username,
    password,
    CONCAT(username, '@admin.vibe'),
    NOW(),
    NOW(),
    0,
    1
FROM tb_admin a
WHERE NOT EXISTS (
    SELECT 1 FROM tb_user u WHERE u.username = a.username
);
