-- ============================================
-- AI 评论机器人用户初始化脚本
-- ============================================

-- 创建 AI 机器人用户 WHUTGPT
-- 密码为随机生成，该用户不需要登录
INSERT INTO `user` (
    `username`, 
    `password`, 
    `email`, 
    `real_name`, 
    `avatar`, 
    `status`
) VALUES (
    'WHUTGPT',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH',  -- 随机密码
    'ai-robot@whut.edu.cn',
    'WHUTGPT',
    '/avatars/ai-robot.png',  -- AI机器人头像
    1
) ON DUPLICATE KEY UPDATE `real_name` = 'WHUTGPT';

-- 查询确认
SELECT id, username, real_name, avatar FROM `user` WHERE username = 'WHUTGPT';
