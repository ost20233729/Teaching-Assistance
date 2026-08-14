package com.java_web.backend.service;

import com.java_web.backend.Common.Entity.Course;
import com.java_web.backend.Common.Entity.CourseObjective;
import com.java_web.backend.Common.Entity.Courseware;
import com.java_web.backend.Common.Entity.Syllabus;
import com.java_web.backend.Common.Mapper.CourseObjectMapper;
import com.java_web.backend.Common.Mapper.CoursewareMapper;
import com.java_web.backend.Common.Mapper.SyllabusMapper;
import com.java_web.backend.Common.Service.LLMCallLogService;
import com.java_web.backend.Common.Service.LLMCoursewareService;
import com.java_web.backend.Common.Service.RestrictionService;
import com.java_web.backend.Teacher.Service.ContentVersionService;
import com.java_web.backend.Teacher.Service.CourseService;
import com.java_web.backend.Teacher.Service.CoursewareService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoursewareServiceTest {

    @Mock
    private CoursewareMapper coursewareMapper;

    @Mock
    private CourseObjectMapper courseObjectMapper;

    @Mock
    private SyllabusMapper syllabusMapper;

    @Mock
    private CourseService courseService;

    @Mock
    private RestrictionService restrictionService;

    @Mock
    private LLMCoursewareService llmCoursewareService;

    @Mock
    private LLMCallLogService llmCallLogService;

    @Mock
    private ContentVersionService contentVersionService;

    @InjectMocks
    private CoursewareService coursewareService;

    @Test
    void generateCoursewareContent_ShouldReturnGeneratedMarkdown() {
        Course course = new Course();
        course.setId(12);
        course.setTeacherId(2);
        course.setName("人工智能基础");
        course.setStatus("approved");

        CourseObjective objective = new CourseObjective();
        objective.setCourseId(12);
        objective.setCourseContent("课程介绍");
        objective.setTeachingTarget("教学目标");

        Syllabus syllabus = new Syllabus();
        syllabus.setCourseId(12);
        syllabus.setContent("# 课程大纲");

        when(courseService.validateApprovedCourse(12, 2)).thenReturn(course);
        when(courseObjectMapper.selectById(12)).thenReturn(objective);
        when(syllabusMapper.selectById(12)).thenReturn(syllabus);
        when(llmCallLogService.summarizePrompt(eq("courseware_generation"), eq("生成课件提纲")))
                .thenReturn("summary");
        when(llmCoursewareService.generateCourseware(
                eq("人工智能基础"),
                eq("课程介绍"),
                eq("教学目标"),
                eq("# 课程大纲"),
                eq("生成课件提纲")
        )).thenReturn("# 教学课件");

        String result = coursewareService.generateCoursewareContent(12, "生成课件提纲", 2);

        assertEquals("# 教学课件", result);
        verify(restrictionService).ensureFunctionAvailable(2, "courseware");
        verify(llmCallLogService).recordSuccess(2, 12, LLMCallLogService.MODULE_COURSEWARE, "summary");
    }

    @Test
    void saveCourseware_ShouldInsertWhenNotExists() {
        Courseware courseware = new Courseware();
        courseware.setCourseId(18);
        courseware.setContent("# 课件提纲");

        when(coursewareMapper.selectById(18)).thenReturn(null);

        Courseware saved = coursewareService.saveCourseware(courseware, 3);

        assertEquals("# 课件提纲", saved.getContent());
        verify(courseService).validateApprovedCourse(18, 3);
        verify(restrictionService).ensureFunctionAvailable(3, "courseware");
        verify(coursewareMapper).insert(any(Courseware.class));
        verify(contentVersionService).recordCoursewareVersion(saved, 3);
    }
}
