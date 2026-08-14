package com.java_web.backend.Teacher.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.java_web.backend.Common.DTO.CourseExportDTO;
import com.java_web.backend.Common.DTO.PagedResponse;
import com.java_web.backend.Common.Entity.Course;
import com.java_web.backend.Common.Entity.CourseObjective;
import com.java_web.backend.Common.Entity.Courseware;
import com.java_web.backend.Common.Entity.Material;
import com.java_web.backend.Common.Entity.Syllabus;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.CourseMapper;
import com.java_web.backend.Common.Mapper.CourseObjectMapper;
import com.java_web.backend.Common.Mapper.CoursewareMapper;
import com.java_web.backend.Common.Mapper.MaterialMapper;
import com.java_web.backend.Common.Mapper.SyllabusMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CourseService {
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern MARKDOWN_HEADING_PATTERN = Pattern.compile("^(#{1,6})(\\s+.*)$");
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 8;
    private static final int MAX_PAGE_SIZE = 50;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CourseObjectMapper objectiveMapper;

    @Autowired
    private SyllabusMapper syllabusMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private CoursewareMapper coursewareMapper;

    public PagedResponse<Course> getTeacherCourses(Integer teacherId,
                                                   String keyword,
                                                   String status,
                                                   Integer page,
                                                   Integer pageSize) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedStatus = normalizeStatusFilter(status);
        long resolvedPage = resolvePage(page);
        long resolvedPageSize = resolvePageSize(pageSize);

        LambdaQueryWrapper<Course> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Course::getTeacherId, teacherId)
                .eq(Course::getIsDeleted, 0);

        if (normalizedKeyword != null) {
            queryWrapper.like(Course::getName, normalizedKeyword);
        }

        if (normalizedStatus != null) {
            queryWrapper.eq(Course::getStatus, normalizedStatus);
        }

        queryWrapper.orderByDesc(Course::getUpdatedAt)
                .orderByDesc(Course::getCreatedAt)
                .orderByDesc(Course::getId);

        Page<Course> resultPage = courseMapper.selectPage(new Page<>(resolvedPage, resolvedPageSize), queryWrapper);
        return PagedResponse.of(resultPage);
    }

    public Map<String, Object> getCourseDetail(Integer courseId, Integer teacherId) {
        Course course = validateCourseOwnership(courseId, teacherId);
        CourseObjective objective = objectiveMapper.selectById(courseId);
        Syllabus syllabus = syllabusMapper.selectById(courseId);
        Material material = materialMapper.selectById(courseId);
        Courseware courseware = coursewareMapper.selectById(courseId);

        Map<String, Object> result = new HashMap<>();
        result.put("course", course);
        result.put("objective", objective);
        result.put("syllabus", syllabus);
        result.put("material", material);
        result.put("courseware", courseware);
        return result;
    }

    public CourseExportDTO exportCourseMarkdown(Integer courseId, Integer teacherId) {
        Course course = validateCourseOwnership(courseId, teacherId);
        CourseObjective objective = objectiveMapper.selectById(courseId);
        Syllabus syllabus = syllabusMapper.selectById(courseId);
        Material material = materialMapper.selectById(courseId);
        Courseware courseware = coursewareMapper.selectById(courseId);

        String fileName = sanitizeFileName(course.getName()) + "-课程成果.md";
        String content = buildExportMarkdown(course, objective, syllabus, material, courseware);
        return new CourseExportDTO(fileName, content);
    }

    public Course createCourse(String courseName, Integer teacherId) {
        if (courseName == null || courseName.trim().isEmpty()) {
            throw ApiException.badRequest("课程名称不能为空");
        }

        Course course = new Course();
        course.setName(courseName.trim());
        course.setTeacherId(teacherId);
        course.setStatus("pending");
        course.setIsDeleted(0);
        course.setCreatedAt(new Date());
        course.setUpdatedAt(new Date());
        courseMapper.insert(course);
        return course;
    }

    public Course updateCourseName(Integer courseId, String newCourseName, Integer teacherId) {
        if (newCourseName == null || newCourseName.trim().isEmpty()) {
            throw ApiException.badRequest("课程名称不能为空");
        }

        Course course = validateCourseOwnership(courseId, teacherId);
        course.setName(newCourseName.trim());
        course.setUpdatedAt(new Date());
        courseMapper.updateById(course);
        return course;
    }

    public void deleteCourse(Integer courseId, Integer teacherId) {
        Course course = validateCourseOwnership(courseId, teacherId);
        course.setIsDeleted(1);
        course.setUpdatedAt(new Date());
        courseMapper.updateById(course);
    }

    public Course validateCourseOwnership(Integer courseId, Integer teacherId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || Integer.valueOf(1).equals(course.getIsDeleted())) {
            throw ApiException.notFound("课程不存在");
        }

        if (!course.getTeacherId().equals(teacherId)) {
            throw ApiException.forbidden("无权操作该课程");
        }

        return course;
    }

    public Course validateApprovedCourse(Integer courseId, Integer teacherId) {
        Course course = validateCourseOwnership(courseId, teacherId);
        if (!"approved".equals(course.getStatus())) {
            throw ApiException.forbidden("课程尚未审核通过，暂不能使用该模块");
        }
        return course;
    }

    private String buildExportMarkdown(Course course,
                                       CourseObjective objective,
                                       Syllabus syllabus,
                                       Material material,
                                       Courseware courseware) {
        String courseName = valueOrDefault(course.getName(), "未命名课程");
        String introduction = readObjectiveContent(objective);
        String teachingTarget = readTeachingTarget(objective);
        String syllabusContent = readSyllabusContent(syllabus);
        String materialContent = readMaterialContent(material);
        String coursewareContent = readCoursewareContent(courseware);

        List<String> completedSections = new ArrayList<>();
        List<String> missingSections = new ArrayList<>();
        collectSectionStatus(completedSections, missingSections, "课程介绍", introduction);
        collectSectionStatus(completedSections, missingSections, "教学目标", teachingTarget);
        collectSectionStatus(completedSections, missingSections, "课程大纲", syllabusContent);
        collectSectionStatus(completedSections, missingSections, "教学讲义", materialContent);
        collectSectionStatus(completedSections, missingSections, "教学课件提纲", coursewareContent);

        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(courseName).append(" 课程成果\n\n");
        builder.append("> 本文件为当前课程的 Markdown 导出结果，包含已保存内容、审核信息和未完成章节提示。\n\n");

        appendExportSummary(builder, course, completedSections, missingSections);
        appendCompletionSummary(builder, introduction, teachingTarget, syllabusContent, materialContent, coursewareContent);
        appendExportSection(builder, "课程介绍", introduction, objective == null ? null : objective.getUpdatedAt(), "当前章节尚未生成或保存课程介绍。");
        appendExportSection(builder, "教学目标", teachingTarget, objective == null ? null : objective.getUpdatedAt(), "当前章节尚未生成或保存教学目标。");
        appendExportSection(builder, "课程大纲", syllabusContent, syllabus == null ? null : syllabus.getUpdatedAt(), "当前章节尚未生成或保存课程大纲。");
        appendExportSection(builder, "教学讲义", materialContent, material == null ? null : material.getUpdatedAt(), "当前章节尚未生成或保存教学讲义。");
        appendExportSection(builder, "教学课件提纲", coursewareContent, courseware == null ? null : courseware.getUpdatedAt(), "当前章节尚未生成或保存教学课件提纲。");

        return builder.toString().trim() + '\n';
    }

    private void appendExportSummary(StringBuilder builder,
                                     Course course,
                                     List<String> completedSections,
                                     List<String> missingSections) {
        builder.append("## 导出摘要\n\n");
        builder.append("- 课程名称：").append(valueOrDefault(course.getName(), "未命名课程")).append('\n');
        builder.append("- 课程 ID：").append(course.getId()).append('\n');
        builder.append("- 审核状态：").append(getExportStatusLabel(course.getStatus())).append('\n');
        builder.append("- 审核意见：").append(valueOrDefault(course.getReviewComment(), "暂无审核意见")).append('\n');
        builder.append("- 审核时间：").append(formatDate(course.getReviewedAt())).append('\n');
        builder.append("- 创建时间：").append(formatDate(course.getCreatedAt())).append('\n');
        builder.append("- 最后更新时间：").append(formatDate(course.getUpdatedAt())).append('\n');
        builder.append("- 导出时间：").append(LocalDateTime.now().format(EXPORT_TIME_FORMATTER)).append('\n');
        builder.append("- 已完成章节：").append(completedSections.size()).append("/5（").append(joinSectionNames(completedSections)).append("）\n");
        builder.append("- 未完成章节：").append(joinSectionNames(missingSections)).append("\n\n");

        String statusHint = buildExportStatusHint(course.getStatus());
        if (statusHint != null) {
            builder.append("> ").append(statusHint).append("\n\n");
        }
    }

    private void appendCompletionSummary(StringBuilder builder,
                                         String introduction,
                                         String teachingTarget,
                                         String syllabusContent,
                                         String materialContent,
                                         String coursewareContent) {
        builder.append("## 章节完成情况\n\n");
        builder.append("- 课程介绍：").append(buildSectionCompletionLine(introduction)).append('\n');
        builder.append("- 教学目标：").append(buildSectionCompletionLine(teachingTarget)).append('\n');
        builder.append("- 课程大纲：").append(buildSectionCompletionLine(syllabusContent)).append('\n');
        builder.append("- 教学讲义：").append(buildSectionCompletionLine(materialContent)).append('\n');
        builder.append("- 教学课件提纲：").append(buildSectionCompletionLine(coursewareContent)).append("\n\n");
    }

    private void appendExportSection(StringBuilder builder,
                                     String sectionTitle,
                                     String content,
                                     Date updatedAt,
                                     String missingHint) {
        boolean hasContent = hasContent(content);

        builder.append("## ").append(sectionTitle).append("\n\n");
        builder.append("> 状态：").append(hasContent ? "已完成" : "未完成").append('\n');
        builder.append("> 最后更新时间：").append(hasContent ? formatDate(updatedAt) : "暂无").append('\n');

        if (!hasContent) {
            builder.append("> 缺失提示：").append(missingHint).append("\n\n");
            return;
        }

        builder.append('\n');
        builder.append(normalizeMarkdownHierarchy(content)).append("\n\n");
    }

    private void collectSectionStatus(List<String> completedSections,
                                      List<String> missingSections,
                                      String sectionName,
                                      String content) {
        if (hasContent(content)) {
            completedSections.add(sectionName);
            return;
        }

        missingSections.add(sectionName);
    }

    private String buildSectionCompletionLine(String content) {
        return hasContent(content)
                ? "已完成，导出时将输出已保存内容"
                : "未完成，导出时保留缺失提示";
    }

    private String joinSectionNames(List<String> sectionNames) {
        if (sectionNames.isEmpty()) {
            return "无";
        }

        return String.join("、", sectionNames);
    }

    private String getExportStatusLabel(String status) {
        if ("approved".equals(status)) {
            return "已审核通过";
        }
        if ("pending".equals(status)) {
            return "待审核";
        }
        if ("rejected".equals(status)) {
            return "已驳回";
        }
        return valueOrDefault(status, "未知");
    }

    private String buildExportStatusHint(String status) {
        if ("approved".equals(status)) {
            return "当前课程已审核通过，导出文件可直接用于展示、留档或继续完善。";
        }
        if ("pending".equals(status)) {
            return "当前课程仍处于待审核阶段，本次导出主要用于阶段性查看和继续完善。";
        }
        if ("rejected".equals(status)) {
            return "当前课程已被驳回，建议优先根据审核意见修改后再次提交。";
        }
        return null;
    }

    private String normalizeMarkdownHierarchy(String content) {
        String[] lines = content.trim().split("\\r?\\n", -1);
        StringBuilder normalized = new StringBuilder();

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            Matcher matcher = MARKDOWN_HEADING_PATTERN.matcher(line);

            if (matcher.matches()) {
                int nextLevel = Math.min(6, matcher.group(1).length() + 2);
                normalized.append("#".repeat(nextLevel)).append(matcher.group(2));
            } else {
                normalized.append(line);
            }

            if (index < lines.length - 1) {
                normalized.append('\n');
            }
        }

        return normalized.toString();
    }

    private String readObjectiveContent(CourseObjective objective) {
        return objective == null ? null : objective.getCourseContent();
    }

    private String readTeachingTarget(CourseObjective objective) {
        return objective == null ? null : objective.getTeachingTarget();
    }

    private String readSyllabusContent(Syllabus syllabus) {
        return syllabus == null ? null : syllabus.getContent();
    }

    private String readMaterialContent(Material material) {
        return material == null ? null : material.getContent();
    }

    private String readCoursewareContent(Courseware courseware) {
        return courseware == null ? null : courseware.getContent();
    }

    private boolean hasContent(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String formatDate(Date value) {
        if (value == null) {
            return "暂无";
        }

        return EXPORT_TIME_FORMATTER.format(value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    private String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    private String sanitizeFileName(String courseName) {
        String baseName = valueOrDefault(courseName, "course");
        return baseName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String trimmedKeyword = keyword.trim();
        return trimmedKeyword.isEmpty() ? null : trimmedKeyword;
    }

    private String normalizeStatusFilter(String status) {
        if (status == null) {
            return null;
        }

        String trimmedStatus = status.trim();
        if (trimmedStatus.isEmpty() || "all".equals(trimmedStatus)) {
            return null;
        }

        if (!"pending".equals(trimmedStatus) && !"approved".equals(trimmedStatus) && !"rejected".equals(trimmedStatus)) {
            throw ApiException.badRequest("非法的课程状态");
        }

        return trimmedStatus;
    }

    private long resolvePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }

        if (page < 1) {
            throw ApiException.badRequest("页码必须大于 0");
        }

        return page;
    }

    private long resolvePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }

        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("每页条数必须在 1 到 50 之间");
        }

        return pageSize;
    }
}
