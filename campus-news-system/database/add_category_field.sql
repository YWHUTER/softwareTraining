-- 为 article 表添加 category 字段（用于校园集市分类）
-- 执行此脚本前请确保已备份数据库

ALTER TABLE `article` 
ADD COLUMN `category` VARCHAR(50) NULL COMMENT '分类（用于校园集市）: daily/trade/help/activity/lost/study/sports' 
AFTER `board_type`;

-- 验证字段是否添加成功
DESCRIBE `article`;
