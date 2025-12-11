-- ============================================
-- 校园新闻发布系统 - 完整数据库脚本
-- MySQL Version: 8.0+
-- 最后更新: 2025-12-06
-- ============================================

-- 创建数据库
DROP DATABASE IF EXISTS campus_news_system;
CREATE DATABASE campus_news_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_news_system;

-- ============================================
-- 表结构
-- ============================================

-- 1. 角色表
CREATE TABLE `role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `role_name` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称: ADMIN/TEACHER/STUDENT',
    `description` TEXT COMMENT '角色描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 2. 学院表
CREATE TABLE `college` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL COMMENT '学院名称',
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '学院代码',
    `description` TEXT COMMENT '学院简介',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学院表';

-- 3. 用户表
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    `email` VARCHAR(100) COMMENT '邮箱',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `phone` VARCHAR(20) COMMENT '手机号',
    `avatar` VARCHAR(500) COMMENT '头像URL',
    `college_id` BIGINT COMMENT '所属学院ID',
    `student_id` VARCHAR(50) COMMENT '学号/工号',
    `following_count` INT DEFAULT 0 COMMENT '关注数',
    `follower_count` INT DEFAULT 0 COMMENT '粉丝数',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_college` (`college_id`),
    INDEX `idx_username` (`username`),
    FOREIGN KEY (`college_id`) REFERENCES `college`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 4. 用户角色关联表
CREATE TABLE `user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user` (`user_id`),
    INDEX `idx_role` (`role_id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`role_id`) REFERENCES `role`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 5. 用户关注表
CREATE TABLE `user_follow` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `follower_id` BIGINT NOT NULL COMMENT '关注者ID（粉丝）',
    `following_id` BIGINT NOT NULL COMMENT '被关注者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_follow` (`follower_id`, `following_id`),
    INDEX `idx_follower` (`follower_id`),
    INDEX `idx_following` (`following_id`),
    FOREIGN KEY (`follower_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`following_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注关系表';

-- 6. 文章表（新闻/帖子统一管理）
CREATE TABLE `article` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` LONGTEXT NOT NULL COMMENT '内容',
    `summary` VARCHAR(500) COMMENT '摘要',
    `cover_image` VARCHAR(1000) COMMENT '封面图URL',
    `author_id` BIGINT NOT NULL COMMENT '作者ID',
    `board_type` VARCHAR(50) NOT NULL COMMENT '板块类型: OFFICIAL/CAMPUS/COLLEGE',
    `college_id` BIGINT COMMENT '所属学院ID（学院板块必填）',
    `view_count` INT DEFAULT 0 COMMENT '浏览量',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `is_pinned` TINYINT DEFAULT 0 COMMENT '是否置顶: 0-否 1-是',
    `is_approved` TINYINT DEFAULT 0 COMMENT '审核状态: 0-待审核 1-已通过 2-已拒绝',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-已删除 1-正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_author` (`author_id`),
    INDEX `idx_board_type` (`board_type`),
    INDEX `idx_college` (`college_id`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_view_count` (`view_count`),
    INDEX `idx_is_pinned` (`is_pinned`),
    FOREIGN KEY (`author_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`college_id`) REFERENCES `college`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- 7. 评论表
CREATE TABLE `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `parent_id` BIGINT COMMENT '父评论ID',
    `reply_to_user_id` BIGINT COMMENT '回复目标用户ID（多级回复显示"回复 @xxx"）',
    `root_id` BIGINT COMMENT '根评论ID（所有回复归属的顶级评论）',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-已删除 1-正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_article` (`article_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_parent` (`parent_id`),
    INDEX `idx_root` (`root_id`),
    FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`parent_id`) REFERENCES `comment`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`reply_to_user_id`) REFERENCES `user`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`root_id`) REFERENCES `comment`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 8. 文章点赞表
CREATE TABLE `article_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
    INDEX `idx_article` (`article_id`),
    INDEX `idx_user` (`user_id`),
    FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞表';

-- 9. 文章收藏表
CREATE TABLE `article_favorite` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
    INDEX `idx_article` (`article_id`),
    INDEX `idx_user` (`user_id`),
    FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章收藏表';

-- 10. 通知表
CREATE TABLE `notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '接收通知的用户ID',
    `from_user_id` BIGINT COMMENT '触发通知的用户ID',
    `type` VARCHAR(20) NOT NULL COMMENT '通知类型: MENTION-被@提及, COMMENT-评论, LIKE-点赞, FOLLOW-关注',
    `article_id` BIGINT COMMENT '相关文章ID',
    `comment_id` BIGINT COMMENT '相关评论ID',
    `content` VARCHAR(500) NOT NULL COMMENT '通知内容',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读 1-已读',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_is_read` (`is_read`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`from_user_id`) REFERENCES `user`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- 11. 标签表
CREATE TABLE `tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    `use_count` INT DEFAULT 0 COMMENT '使用次数（热度）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_use_count` (`use_count` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- 12. 文章标签关联表
CREATE TABLE `article_tag` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `article_id` BIGINT NOT NULL COMMENT '文章ID',
    `tag_id` BIGINT NOT NULL COMMENT '标签ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`),
    INDEX `idx_article_id` (`article_id`),
    INDEX `idx_tag_id` (`tag_id`),
    FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`tag_id`) REFERENCES `tag`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签关联表';

-- 13. AI聊天会话表
CREATE TABLE `chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(100) NOT NULL COMMENT '会话标题',
    `model` VARCHAR(50) DEFAULT 'kimi' COMMENT '使用的模型',
    `message_count` INT DEFAULT 0 COMMENT '消息数量',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_updated_at` (`updated_at`),
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天会话表';

-- 14. AI聊天消息表
CREATE TABLE `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: user/assistant',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_session_id` (`session_id`),
    FOREIGN KEY (`session_id`) REFERENCES `chat_session`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天消息表';

-- 15. 视频分类表
CREATE TABLE `video_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '分类代码',
    `sort_order` INT DEFAULT 0 COMMENT '排序顺序',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频分类表';

-- 16. 视频表
CREATE TABLE `video` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL COMMENT '视频标题',
    `description` TEXT COMMENT '视频描述',
    `video_url` VARCHAR(500) NOT NULL COMMENT '视频文件URL',
    `thumbnail` VARCHAR(500) COMMENT '缩略图URL',
    `duration` VARCHAR(20) COMMENT '视频时长(格式: HH:MM:SS)',
    `duration_seconds` INT DEFAULT 0 COMMENT '视频时长(秒)',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    `category_id` BIGINT COMMENT '分类ID',
    `author_id` BIGINT NOT NULL COMMENT '上传者ID',
    `channel_name` VARCHAR(100) COMMENT '频道名称',
    `view_count` INT DEFAULT 0 COMMENT '播放量',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT DEFAULT 0 COMMENT '评论数',
    `is_approved` TINYINT DEFAULT 0 COMMENT '审核状态: 0-待审核 1-已通过 2-已拒绝',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-已删除 1-正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_category` (`category_id`),
    INDEX `idx_author` (`author_id`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_view_count` (`view_count`),
    FOREIGN KEY (`category_id`) REFERENCES `video_category`(`id`) ON DELETE SET NULL,
    FOREIGN KEY (`author_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频表';

-- 17. 视频点赞表
CREATE TABLE `video_like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `video_id` BIGINT NOT NULL COMMENT '视频ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_video_user` (`video_id`, `user_id`),
    INDEX `idx_video` (`video_id`),
    INDEX `idx_user` (`user_id`),
    FOREIGN KEY (`video_id`) REFERENCES `video`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频点赞表';

-- ============================================
-- 初始化数据
-- ============================================

-- 初始化角色数据
INSERT INTO `role` (`id`, `role_name`, `description`) VALUES
(1, 'ADMIN', '系统管理员'),
(2, 'TEACHER', '教师'),
(3, 'STUDENT', '学生');

-- 初始化学院数据
INSERT INTO `college` (`id`, `name`, `code`, `description`) VALUES
(1, '计算机科学与技术学院', 'CS', '计算机学院'),
(2, '软件工程学院', 'SE', '软件学院'),
(3, '人工智能学院', 'AI', 'AI学院'),
(4, '电子信息工程学院', 'EE', '电子学院'),
(5, '经济管理学院', 'EM', '经管学院');

-- 初始化管理员账号 (密码: admin123)
INSERT INTO `user` (`id`, `username`, `password`, `email`, `real_name`, `status`) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@campus.edu', '系统管理员', 1);

INSERT INTO `user_role` (`user_id`, `role_id`) VALUES (1, 1);

-- 初始化热门标签
INSERT INTO `tag` (`name`, `use_count`) VALUES
('校园活动', 15),
('学术讲座', 12),
('招聘信息', 10),
('社团招新', 9),
('比赛通知', 8),
('奖学金', 7),
('考试安排', 6),
('通知公告', 5),
('科研动态', 4),
('校园风光', 4),
('体育赛事', 3),
('文艺演出', 3),
('志愿服务', 2),
('创新创业', 2),
('国际交流', 1);

-- 初始化视频分类数据
INSERT INTO `video_category` (`name`, `code`, `sort_order`) VALUES
('校园活动', 'activity', 1),
('讲座报告', 'lecture', 2),
('人物访谈', 'interview', 3),
('校园风光', 'campus', 4),
('体育赛事', 'sports', 5),
('文艺演出', 'art', 6),
('科技创新', 'tech', 7),
('校园生活', 'life', 8),
('新闻直击', 'news', 9),
('纪录片', 'documentary', 10),
('Vlog', 'vlog', 11);

-- ============================================
-- 完成
-- ============================================
SELECT '数据库初始化完成！' AS message;
