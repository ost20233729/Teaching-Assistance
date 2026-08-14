package com.java_web.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.java_web.backend.Common.DTO.CourseExportDTO;
import com.java_web.backend.Common.DTO.PagedResponse;
import com.java_web.backend.Common.Entity.Course;
import com.java_web.backend.Common.Entity.CourseObjective;
import com.java_web.backend.Common.Entity.Courseware;
import com.java_web.backend.Common.Entity.Material;
import com.java_web.backend.Common.Entity.Syllabus;
import com.java_web.backend.Common.Mapper.CourseMapper;
import com.java_web.backend.Common.Mapper.CourseObjectMapper;
import com.java_web.backend.Common.Mapper.CoursewareMapper;
import com.java_web.backend.Common.Mapper.MaterialMapper;
import com.java_web.backend.Common.Mapper.SyllabusMapper;
import com.java_web.backend.Teacher.Service.CourseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private CourseObjectMapper objectiveMapper;

    @Mock
    private SyllabusMapper syllabusMapper;

    @Mock
    private MaterialMapper materialMapper;

    @Mock
    private CoursewareMapper coursewareMapper;

    @InjectMocks
    private CourseService courseService;

    @Test
    void getTeacherCourses_ShouldReturnPagedResponse() {
        Course course = new Course();
        course.setId(12);
        course.setTeacherId(1);
        course.setName("人工智能基础");
        course.setStatus("approved");
        course.setIsDeleted(0);

        Page<Course> pageData = new Page<>(2, 8);
        pageData.setRecords(List.of(course));
        pageData.setTotal(9);

        when(courseMapper.selectPage(any(Page.class), any())).thenReturn(pageData);

        PagedResponse<Course> response = courseService.getTeacherCourses(1, "人工", "approved", 2, 8);

        assertEquals(1, response.getItems().size());
        assertEquals("人工智能基础", response.getItems().get(0).getName());
        assertEquals(2, response.getPage());
        assertEquals(8, response.getPageSize());
        assertEquals(9, response.getTotal());
        assertEquals(2, response.getTotalPages());
        verify(courseMapper).selectPage(argThat(page -> page.getCurrent() == 2 && page.getSize() == 8), any());
    }

    @Test
    void exportCourseMarkdown_ShouldIncludeSummaryStatusAndNormalizedHierarchy() {
        Course course = new Course();
        course.setId(12);
        course.setTeacherId(1);
        course.setName("人工智能基础");
        course.setStatus("approved");
        course.setReviewComment("教学目标完整，可继续生成后续内容。");
        course.setReviewedAt(asDate(2026, 5, 11, 9, 0));
        course.setCreatedAt(asDate(2026, 5, 10, 10, 0));
        course.setUpdatedAt(asDate(2026, 5, 11, 11, 30));
        course.setIsDeleted(0);

        CourseObjective objective = new CourseObjective();
        objective.setCourseId(12);
        objective.setCourseContent("本课程介绍人工智能基础概念。");
        objective.setTeachingTarget("掌握机器学习与知识表示的基础思路。");
        objective.setUpdatedAt(asDate(2026, 5, 11, 8, 30));

        Syllabus syllabus = new Syllabus();
        syllabus.setCourseId(12);
        syllabus.setContent("# 课程安排\n\n## 第一周 导论");
        syllabus.setUpdatedAt(asDate(2026, 5, 11, 8, 40));

        Material material = new Material();
        material.setCourseId(12);
        material.setContent("# 教学讲义\n\n## 核心概念");
        material.setUpdatedAt(asDate(2026, 5, 11, 8, 50));

        Courseware courseware = new Courseware();
        courseware.setCourseId(12);
        courseware.setContent("# 课件提纲\n\n## 第 1 页 课程定位");
        courseware.setUpdatedAt(asDate(2026, 5, 11, 9, 10));

        when(courseMapper.selectById(12)).thenReturn(course);
        when(objectiveMapper.selectById(12)).thenReturn(objective);
        when(syllabusMapper.selectById(12)).thenReturn(syllabus);
        when(materialMapper.selectById(12)).thenReturn(material);
        when(coursewareMapper.selectById(12)).thenReturn(courseware);

        CourseExportDTO export = courseService.exportCourseMarkdown(12, 1);

        assertEquals("人工智能基础-课程成果.md", export.getFileName());
        assertTrue(export.getContent().contains("# 人工智能基础 课程成果"));
        assertTrue(export.getContent().contains("## 导出摘要"));
        assertTrue(export.getContent().contains("审核状态：已审核通过"));
        assertTrue(export.getContent().contains("审核意见：教学目标完整，可继续生成后续内容。"));
        assertTrue(export.getContent().contains("- 已完成章节：5/5（课程介绍、教学目标、课程大纲、教学讲义、教学课件提纲）"));
        assertTrue(export.getContent().contains("## 章节完成情况"));
        assertTrue(export.getContent().contains("## 课程介绍"));
        assertTrue(export.getContent().contains("> 状态：已完成"));
        assertTrue(export.getContent().contains("### 课程安排"));
        assertTrue(export.getContent().contains("#### 第一周 导论"));
        assertTrue(export.getContent().contains("### 课件提纲"));
        assertTrue(export.getContent().contains("#### 第 1 页 课程定位"));
    }

    @Test
    void exportCourseMarkdown_ShouldShowClearMissingHintsWhenContentIsMissing() {
        Course course = new Course();
        course.setId(20);
        course.setTeacherId(2);
        course.setName("软件工程");
        course.setStatus("pending");
        course.setCreatedAt(asDate(2026, 5, 11, 9, 0));
        course.setUpdatedAt(asDate(2026, 5, 11, 9, 0));
        course.setIsDeleted(0);

        when(courseMapper.selectById(20)).thenReturn(course);
        when(objectiveMapper.selectById(20)).thenReturn(null);
        when(syllabusMapper.selectById(20)).thenReturn(null);
        when(materialMapper.selectById(20)).thenReturn(null);
        when(coursewareMapper.selectById(20)).thenReturn(null);

        CourseExportDTO export = courseService.exportCourseMarkdown(20, 2);

        assertEquals("软件工程-课程成果.md", export.getFileName());
        assertTrue(export.getContent().contains("审核状态：待审核"));
        assertTrue(export.getContent().contains("当前课程仍处于待审核阶段，本次导出主要用于阶段性查看和继续完善。"));
        assertTrue(export.getContent().contains("- 未完成章节：课程介绍、教学目标、课程大纲、教学讲义、教学课件提纲"));
        assertTrue(export.getContent().contains("> 缺失提示：当前章节尚未生成或保存课程介绍。"));
        assertTrue(export.getContent().contains("> 缺失提示：当前章节尚未生成或保存教学目标。"));
        assertTrue(export.getContent().contains("> 缺失提示：当前章节尚未生成或保存课程大纲。"));
        assertTrue(export.getContent().contains("> 缺失提示：当前章节尚未生成或保存教学讲义。"));
        assertTrue(export.getContent().contains("> 缺失提示：当前章节尚未生成或保存教学课件提纲。"));
    }

    private Date asDate(int year, int month, int day, int hour, int minute) {
        return Date.from(LocalDateTime.of(year, month, day, hour, minute)
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }
}
