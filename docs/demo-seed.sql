-- Teaching-Assistance 演示数据初始化脚本
-- 适用场景：空库恢复后的本地演示、课程答辩、联调验收
-- 使用方式：
--   1. 先执行 `docs/database.sql`
--   2. 再执行本脚本
-- 说明：
--   - 本脚本只重建约定的演示账号与演示课程，不处理其它业务账号
--   - 演示账号密码为明文 `123456`，仅限开发/答辩环境使用

USE `teaching_assistance`;

-- 确保基础演示账号存在
INSERT INTO `user` (`username`, `password`, `email`, `role`, `status`, `is_deleted`)
VALUES ('admin', '123456', 'admin@example.com', 'admin', 'active', 0)
ON DUPLICATE KEY UPDATE
    `username` = VALUES(`username`),
    `password` = VALUES(`password`),
    `role` = VALUES(`role`),
    `status` = VALUES(`status`),
    `is_deleted` = 0,
    `updated_at` = CURRENT_TIMESTAMP;

INSERT INTO `user` (`username`, `password`, `email`, `role`, `status`, `is_deleted`)
VALUES ('teacher', '123456', 'teacher@example.com', 'teacher', 'active', 0)
ON DUPLICATE KEY UPDATE
    `username` = VALUES(`username`),
    `password` = VALUES(`password`),
    `role` = VALUES(`role`),
    `status` = VALUES(`status`),
    `is_deleted` = 0,
    `updated_at` = CURRENT_TIMESTAMP;

SET @teacher_id = (
    SELECT `id`
    FROM `user`
    WHERE `email` = 'teacher@example.com' AND `is_deleted` = 0
    ORDER BY `id`
    LIMIT 1
);

SET @approved_course_name = '软件工程基础（演示已通过）';
SET @pending_course_name = 'Python 程序设计（演示待审核）';

SET @existing_approved_course_id = (
    SELECT `id`
    FROM `course`
    WHERE `teacher_id` = @teacher_id AND `name` = @approved_course_name AND `is_deleted` = 0
    ORDER BY `id`
    LIMIT 1
);

SET @existing_pending_course_id = (
    SELECT `id`
    FROM `course`
    WHERE `teacher_id` = @teacher_id AND `name` = @pending_course_name AND `is_deleted` = 0
    ORDER BY `id`
    LIMIT 1
);

-- 清理本脚本约定的演示数据，保留无关账号和课程
DELETE FROM `llm_call_log`
WHERE `request_summary` LIKE '[演示]%'
   OR `course_id` IN (@existing_approved_course_id, @existing_pending_course_id);

DELETE FROM `course_content_version`
WHERE `course_id` IN (@existing_approved_course_id, @existing_pending_course_id);

DELETE FROM `notification`
WHERE `user_id` = @teacher_id
  AND (
    `content` LIKE '%软件工程基础（演示已通过）%'
    OR `content` LIKE '%课程大纲”模块的限制%'
  );

DELETE FROM `course`
WHERE `teacher_id` = @teacher_id
  AND `name` IN (@approved_course_name, @pending_course_name);

-- 重建演示课程
INSERT INTO `course` (
    `teacher_id`, `name`, `status`, `review_comment`, `reviewed_at`, `is_deleted`, `created_at`, `updated_at`
)
VALUES
(
    @teacher_id,
    @approved_course_name,
    'approved',
    '课程定位清晰，教学目标完整，同意进入内容生成与导出环节。',
    NOW() - INTERVAL 2 DAY,
    0,
    NOW() - INTERVAL 4 DAY,
    NOW() - INTERVAL 2 DAY
),
(
    @teacher_id,
    @pending_course_name,
    'pending',
    NULL,
    NULL,
    0,
    NOW() - INTERVAL 1 DAY,
    NOW() - INTERVAL 1 DAY
);

SET @approved_course_id = (
    SELECT `id`
    FROM `course`
    WHERE `teacher_id` = @teacher_id AND `name` = @approved_course_name AND `is_deleted` = 0
    ORDER BY `id`
    LIMIT 1
);

SET @pending_course_id = (
    SELECT `id`
    FROM `course`
    WHERE `teacher_id` = @teacher_id AND `name` = @pending_course_name AND `is_deleted` = 0
    ORDER BY `id`
    LIMIT 1
);

INSERT INTO `course_objective` (`course_id`, `course_content`, `teaching_target`, `created_at`, `updated_at`)
VALUES (
    @approved_course_id,
    '本课程面向软件工程相关专业学生，聚焦需求分析、系统设计、实现与测试等软件开发核心环节，帮助学生建立完整的软件工程过程观。',
    '1. 理解软件工程生命周期与典型开发流程。 2. 掌握需求分析、设计建模、编码规范与测试基础。 3. 能结合案例完成小型软件项目的过程设计与文档整理。',
    NOW() - INTERVAL 3 DAY,
    NOW() - INTERVAL 2 DAY
);

INSERT INTO `syllabus` (`course_id`, `content`, `created_at`, `updated_at`)
VALUES (
    @approved_course_id,
    '# 软件工程基础课程大纲

## 第1周 课程导论
- 软件工程的定义、目标与学习方式

## 第2-4周 需求分析
- 用户需求、功能需求、用例建模

## 第5-8周 系统设计
- 架构设计、模块划分、数据库设计

## 第9-12周 实现与测试
- 编码规范、单元测试、集成测试

## 第13-16周 项目实践与复盘
- 小组项目演示、缺陷复盘与课程总结',
    NOW() - INTERVAL 3 DAY,
    NOW() - INTERVAL 2 DAY
);

INSERT INTO `material` (`course_id`, `content`, `created_at`, `updated_at`)
VALUES (
    @approved_course_id,
    '# 软件工程基础教学讲义

## 一、课程定位
软件工程基础强调“过程 + 规范 + 协作”三位一体。

## 二、核心知识
1. 需求分析
2. 系统设计
3. 编码实现
4. 软件测试

## 三、案例讨论
- 以学生成绩管理系统为例，梳理需求、设计与测试的完整流程。

## 四、课堂活动
- 分组讨论需求冲突
- 绘制系统模块图
- 设计测试用例',
    NOW() - INTERVAL 2 DAY,
    NOW() - INTERVAL 1 DAY
);

INSERT INTO `courseware` (`course_id`, `content`, `created_at`, `updated_at`)
VALUES (
    @approved_course_id,
    '# 教学课件提纲

## 第1页 课程定位
- 软件工程为什么重要
- 本课程解决什么问题

## 第2页 学习目标
- 理解生命周期
- 掌握需求、设计、测试基础

## 第3-5页 需求分析
- 用例分析
- 功能拆解
- 需求变更控制

## 第6-8页 系统设计
- 架构设计
- 数据库设计
- 模块协作

## 第9-10页 实现与测试
- 代码规范
- 测试策略

## 第11页 案例讨论
- 成绩管理系统过程复盘

## 第12页 课堂总结与作业
- 本周重点
- 课后实践任务',
    NOW() - INTERVAL 2 DAY,
    NOW() - INTERVAL 1 DAY
);

INSERT INTO `course_content_version` (`course_id`, `module_type`, `content`, `created_by`, `created_at`)
VALUES
(
    @approved_course_id,
    'objective',
    '版本1：课程面向软件工程专业学生，重点介绍软件开发过程和团队协作。',
    @teacher_id,
    NOW() - INTERVAL 3 DAY
),
(
    @approved_course_id,
    'objective',
    '版本2：补充了教学目标中的需求分析、设计建模与测试能力要求。',
    @teacher_id,
    NOW() - INTERVAL 2 DAY
),
(
    @approved_course_id,
    'syllabus',
    '版本1：完成按周划分的课程大纲初稿，覆盖导论、需求、设计、测试与总结。',
    @teacher_id,
    NOW() - INTERVAL 2 DAY
),
(
    @approved_course_id,
    'syllabus',
    '版本2：补充项目实践和课程复盘安排。',
    @teacher_id,
    NOW() - INTERVAL 1 DAY
),
(
    @approved_course_id,
    'material',
    '版本1：整理课程定位与核心知识点。',
    @teacher_id,
    NOW() - INTERVAL 1 DAY
),
(
    @approved_course_id,
    'material',
    '版本2：补充案例讨论与课堂活动设计。',
    @teacher_id,
    NOW() - INTERVAL 12 HOUR
),
(
    @approved_course_id,
    'courseware',
    '版本1：完成 12 页教学课件提纲初稿，覆盖课程定位、学习目标、需求分析、系统设计、实现测试与课堂总结。',
    @teacher_id,
    NOW() - INTERVAL 10 HOUR
),
(
    @approved_course_id,
    'courseware',
    '版本2：补充案例讨论页、课堂互动设计页和课后任务页，便于课堂演示与答辩展示。',
    @teacher_id,
    NOW() - INTERVAL 6 HOUR
);

INSERT INTO `llm_call_log` (`user_id`, `course_id`, `module_type`, `request_summary`, `status`, `error_message`, `created_at`)
VALUES
(
    @teacher_id,
    @approved_course_id,
    'objective',
    '[演示] 根据软件工程基础课程生成课程介绍与教学目标',
    'success',
    NULL,
    NOW() - INTERVAL 2 DAY
),
(
    @teacher_id,
    @approved_course_id,
    'syllabus',
    '[演示] 根据课程介绍与教学目标生成 16 周教学大纲',
    'success',
    NULL,
    DATE_ADD(NOW() - INTERVAL 2 DAY, INTERVAL 30 MINUTE)
),
(
    @teacher_id,
    @approved_course_id,
    'material',
    '[演示] 根据大纲生成章节讲义初稿',
    'success',
    NULL,
    NOW() - INTERVAL 1 DAY
),
(
    @teacher_id,
    @approved_course_id,
    'courseware',
    '[演示] 根据课程资料生成 12 页教学课件提纲',
    'success',
    NULL,
    NOW() - INTERVAL 20 HOUR
),
(
    @teacher_id,
    @approved_course_id,
    'markdown_conversion',
    '[演示] 将课程成果 JSON 转为 Markdown',
    'success',
    NULL,
    NOW() - INTERVAL 18 HOUR
),
(
    @teacher_id,
    @approved_course_id,
    'markdown_conversion_batch',
    '[演示] 批量转换课程成果摘要',
    'failed',
    '演示用失败记录：批量转换请求中存在空段落。',
    NOW() - INTERVAL 16 HOUR
);

INSERT INTO `notification` (`user_id`, `title`, `content`, `type`, `is_read`, `created_at`)
VALUES
(
    @teacher_id,
    '课程审核已通过',
    '课程《软件工程基础（演示已通过）》已审核通过，可以继续进入课程介绍、课程大纲、教学讲义和教学课件提纲模块。',
    'course_approved',
    0,
    NOW() - INTERVAL 12 HOUR
),
(
    @teacher_id,
    '功能限制已解除',
    '管理员已解除你对“课程大纲”模块的限制，你现在可以继续使用该功能。',
    'restriction_removed',
    1,
    NOW() - INTERVAL 1 DAY
);
