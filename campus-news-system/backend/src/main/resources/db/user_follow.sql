-- 用户关注表
CREATE TABLE IF NOT EXISTS `user_follow` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `follower_id` BIGINT NOT NULL COMMENT '关注者ID（粉丝）',
    `following_id` BIGINT NOT NULL COMMENT '被关注者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    UNIQUE KEY `uk_follow` (`follower_id`, `following_id`),
    INDEX `idx_follower` (`follower_id`),
    INDEX `idx_following` (`following_id`),
    FOREIGN KEY (`follower_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`following_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';

-- 在 user 表中添加关注数和粉丝数字段（如果不存在）
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `following_count` INT DEFAULT 0 COMMENT '关注数';
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `follower_count` INT DEFAULT 0 COMMENT '粉丝数';
