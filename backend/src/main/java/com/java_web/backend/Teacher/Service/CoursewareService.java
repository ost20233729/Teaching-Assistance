package com.java_web.backend.Teacher.Service;

import com.java_web.backend.Common.Entity.Course;
import com.java_web.backend.Common.Entity.CourseObjective;
import com.java_web.backend.Common.Entity.Courseware;
import com.java_web.backend.Common.Entity.Syllabus;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.CourseObjectMapper;
import com.java_web.backend.Common.Mapper.CoursewareMapper;
import com.java_web.backend.Common.Mapper.SyllabusMapper;
import com.java_web.backend.Common.Service.LLMCallLogService;
import com.java_web.backend.Common.Service.LLMCoursewareService;
import com.java_web.backend.Common.Service.RestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class CoursewareService {
    @Autowired
    private CoursewareMapper coursewareMapper;

    @Autowired
    private CourseObjectMapper courseObjectMapper;

    @Autowired
    private SyllabusMapper syllabusMapper;

    @Autowired
    private CourseService courseService;

    @Autowired
    private RestrictionService restrictionService;

    @Autowired
    private LLMCoursewareService llmCoursewareService;

    @Autowired
    private LLMCallLogService llmCallLogService;

    @Autowired
    private ContentVersionService contentVersionService;

    public Courseware getCourseware(Integer courseId, Integer teacherId) {
        courseService.validateApprovedCourse(courseId, teacherId);
        restrictionService.ensureFunctionAvailable(teacherId, "courseware");
        return coursewareMapper.selectById(courseId);
    }

    public String generateCoursewareContent(Integer courseId, String prompt, Integer teacherId) {
        Course course = courseService.validateApprovedCourse(courseId, teacherId);
        restrictionService.ensureFunctionAvailable(teacherId, "courseware");

        CourseObjective objective = courseObjectMapper.selectById(courseId);
        if (objective == null
                || isBlank(objective.getCourseContent())
                || isBlank(objective.getTeachingTarget())) {
            throw ApiException.badRequest("请先完成课程介绍和教学目标");
        }

        Syllabus syllabus = syllabusMapper.selectById(courseId);
        if (syllabus == null || isBlank(syllabus.getContent())) {
            throw ApiException.badRequest("请先完成课程大纲");
        }

        String requestSummary = llmCallLogService.summarizePrompt("courseware_generation", prompt);

        try {
            String content = llmCoursewareService.generateCourseware(
                    course.getName(),
                    objective.getCourseContent(),
                    objective.getTeachingTarget(),
                    syllabus.getContent(),
                    prompt
            );
            llmCallLogService.recordSuccess(
                    teacherId,
                    courseId,
                    LLMCallLogService.MODULE_COURSEWARE,
                    requestSummary
            );
            return content;
        } catch (Exception exception) {
            llmCallLogService.recordFailure(
                    teacherId,
                    courseId,
                    LLMCallLogService.MODULE_COURSEWARE,
                    requestSummary,
                    exception
            );
            throw ApiException.badGateway("教学课件提纲生成失败，请稍后重试");
        }
    }

    public Courseware saveCourseware(Courseware courseware, Integer teacherId) {
        courseService.validateApprovedCourse(courseware.getCourseId(), teacherId);
        restrictionService.ensureFunctionAvailable(teacherId, "courseware");

        if (courseware.getContent() != null) {
            courseware.setContent(courseware.getContent()
                    .replace("\\\\n", "\n")
                    .replace("\\\\t", "\t")
                    .replace("\\\\r", "\r"));
        }

        Courseware existingCourseware = coursewareMapper.selectById(courseware.getCourseId());
        if (existingCourseware == null) {
            courseware.setCreatedAt(new Date());
            courseware.setUpdatedAt(new Date());
            coursewareMapper.insert(courseware);
            contentVersionService.recordCoursewareVersion(courseware, teacherId);
            return courseware;
        }

        existingCourseware.setContent(courseware.getContent());
        existingCourseware.setUpdatedAt(new Date());
        coursewareMapper.updateById(existingCourseware);
        contentVersionService.recordCoursewareVersion(existingCourseware, teacherId);
        return existingCourseware;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
