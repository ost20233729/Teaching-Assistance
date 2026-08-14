-- Teaching-Assistance 数据库初始化脚本
-- 适用场景：本地开发、课程答辩演示、测试环境初始化
-- 说明：本脚本以当前代码实际使用的表结构为准，包含课程审核意见、内容版本历史、LLM 调用日志和教学课件表
-- 说明补充：本脚本负责建表和基础账号，不负责完整演示数据；如需一键恢复答辩演示内容，请继续执行 `docs/demo-seed.sql`
-- 注意：示例账号密码为明文演示写法，生产环境应改为加密存储
--
-- 旧版本数据库升级提示：
-- ALTER TABLE `course` ADD COLUMN `review_comment` VARCHAR(500) NULL COMMENT '审核意见';
-- ALTER TABLE `course` ADD COLUMN `reviewed_at` TIMESTAMP NULL COMMENT '审核时间';
-- CREATE TABLE `courseware` (
--   `course_id` INT PRIMARY KEY,
--   `content` TEXT COMMENT '教学课件提纲内容',
--   `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--   `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--   CONSTRAINT `fk_courseware_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE
-- );
-- CREATE TABLE `course_content_version` (
--   `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
--   `course_id` INT NOT NULL,
--   `module_type` VARCHAR(20) NOT NULL,
--   `content` LONGTEXT NOT NULL,
--   `created_by` INT NOT NULL,
--   `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--   CONSTRAINT `fk_version_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
--   CONSTRAINT `fk_version_user` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE CASCADE,
--   INDEX `idx_version_course_module` (`course_id`, `module_type`),
--   INDEX `idx_version_created_at` (`created_at`)
-- );
-- CREATE TABLE `llm_call_log` (
--   `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
--   `user_id` INT NOT NULL,
--   `course_id` INT NULL,
--   `module_type` VARCHAR(50) NOT NULL,
--   `request_summary` VARCHAR(500) NOT NULL,
--   `status` VARCHAR(20) NOT NULL,
--   `error_message` VARCHAR(500) NULL,
--   `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--   CONSTRAINT `fk_llm_call_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
--   CONSTRAINT `fk_llm_call_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE SET NULL,
--   INDEX `idx_llm_call_module` (`module_type`),
--   INDEX `idx_llm_call_status` (`status`),
--   INDEX `idx_llm_call_created_at` (`created_at`)
-- );
-- CREATE TABLE `notification` (
--   `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
--   `user_id` INT NOT NULL,
--   `title` VARCHAR(100) NOT NULL,
--   `content` VARCHAR(500) NOT NULL,
--   `type` VARCHAR(50) NOT NULL,
--   `is_read` TINYINT(1) NOT NULL DEFAULT 0,
--   `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--   CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
--   INDEX `idx_notification_user` (`user_id`),
--   INDEX `idx_notification_read` (`is_read`),
--   INDEX `idx_notification_created_at` (`created_at`)
-- );

CREATE DATABASE IF NOT EXISTS `teaching_assistance`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `teaching_assistance`;

-- 用户表：存储教师与管理员账号
CREATE TABLE IF NOT EXISTS `user` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（演示环境可明文，生产环境应加密）',
    `email` VARCHAR(100) UNIQUE COMMENT '邮箱',
    `avatar_url` VARCHAR(255) COMMENT '头像 URL',
    `role` VARCHAR(20) NOT NULL COMMENT '用户角色（teacher/admin）',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '账户状态（active/frozen）',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记（0-未删除，1-已删除）',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_email` (`email`),
    INDEX `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 课程表：存储课程主记录、审核状态和审核意见
CREATE TABLE IF NOT EXISTS `course` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `teacher_id` INT NOT NULL COMMENT '教师 ID',
    `name` VARCHAR(100) NOT NULL COMMENT '课程名称',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '审核状态（pending/approved/rejected）',
    `review_comment` VARCHAR(500) NULL COMMENT '审核意见',
    `reviewed_at` TIMESTAMP NULL COMMENT '审核时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT `fk_course_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 课程介绍与教学目标表
CREATE TABLE IF NOT EXISTS `course_objective` (
    `course_id` INT PRIMARY KEY COMMENT '课程 ID',
    `course_content` TEXT COMMENT '课程介绍',
    `teaching_target` TEXT COMMENT '教学目标',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT `fk_objective_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程目标表';

-- 课程大纲表
CREATE TABLE IF NOT EXISTS `syllabus` (
    `course_id` INT PRIMARY KEY COMMENT '课程 ID',
    `content` TEXT COMMENT '课程大纲内容',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT `fk_syllabus_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程大纲表';

-- 教学讲义表
CREATE TABLE IF NOT EXISTS `material` (
    `course_id` INT PRIMARY KEY COMMENT '课程 ID',
    `content` TEXT COMMENT '讲义内容',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT `fk_material_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='讲义表';

-- 教学课件表：存储课件提纲 Markdown
CREATE TABLE IF NOT EXISTS `courseware` (
    `course_id` INT PRIMARY KEY COMMENT '课程 ID',
    `content` TEXT COMMENT '教学课件提纲内容',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT `fk_courseware_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学课件表';

-- 课程内容版本表：统一存储课程介绍/目标、大纲、讲义的历史快照
CREATE TABLE IF NOT EXISTS `course_content_version` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `course_id` INT NOT NULL COMMENT '课程 ID',
    `module_type` VARCHAR(20) NOT NULL COMMENT '模块类型（objective/syllabus/material/courseware）',
    `content` LONGTEXT NOT NULL COMMENT '历史快照内容',
    `created_by` INT NOT NULL COMMENT '创建该版本的教师 ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '版本创建时间',
    CONSTRAINT `fk_version_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_version_user` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_version_course_module` (`course_id`, `module_type`),
    INDEX `idx_version_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程内容历史版本表';

-- LLM 调用日志表：记录目标/大纲/讲义/课件生成和 Markdown 转换调用情况
CREATE TABLE IF NOT EXISTS `llm_call_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL COMMENT '调用用户 ID',
    `course_id` INT NULL COMMENT '关联课程 ID',
    `module_type` VARCHAR(50) NOT NULL COMMENT '调用模块类型（objective/syllabus/material/courseware/markdown_conversion/markdown_conversion_batch）',
    `request_summary` VARCHAR(500) NOT NULL COMMENT '请求摘要',
    `status` VARCHAR(20) NOT NULL COMMENT '调用状态（success/failed）',
    `error_message` VARCHAR(500) NULL COMMENT '失败原因',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    CONSTRAINT `fk_llm_call_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_llm_call_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE SET NULL,
    INDEX `idx_llm_call_module` (`module_type`),
    INDEX `idx_llm_call_status` (`status`),
    INDEX `idx_llm_call_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM 调用日志表';

-- 功能限制表
CREATE TABLE IF NOT EXISTS `restriction` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL COMMENT '用户 ID',
    `function_name` VARCHAR(50) NOT NULL COMMENT '功能标识',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT `fk_restriction_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_user_function` (`user_id`, `function_name`),
    INDEX `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能限制表';

-- 站内通知表：存储审核提醒与功能限制变更提醒
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` INT NOT NULL COMMENT '接收通知的用户 ID',
    `title` VARCHAR(100) NOT NULL COMMENT '通知标题',
    `content` VARCHAR(500) NOT NULL COMMENT '通知内容',
    `type` VARCHAR(50) NOT NULL COMMENT '通知类型（course_approved/course_rejected/restriction_added/restriction_removed）',
    `is_read` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已读（0-未读，1-已读）',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    INDEX `idx_notification_user` (`user_id`),
    INDEX `idx_notification_read` (`is_read`),
    INDEX `idx_notification_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';

-- 初始管理员账号
INSERT INTO `user` (`username`, `password`, `email`, `role`, `status`)
VALUES ('admin', '123456', 'admin@example.com', 'admin', 'active')
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `role` = VALUES(`role`),
    `status` = VALUES(`status`),
    `updated_at` = CURRENT_TIMESTAMP;

-- 示例教师账号
INSERT INTO `user` (`username`, `password`, `email`, `role`, `status`)
VALUES ('teacher', '123456', 'teacher@example.com', 'teacher', 'active')
ON DUPLICATE KEY UPDATE
    `password` = VALUES(`password`),
    `role` = VALUES(`role`),
    `status` = VALUES(`status`),
    `updated_at` = CURRENT_TIMESTAMP;
