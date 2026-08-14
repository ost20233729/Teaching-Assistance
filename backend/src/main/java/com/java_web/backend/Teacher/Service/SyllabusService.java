package com.java_web.backend.Teacher.Service;

import com.java_web.backend.Common.DTO.SyllabusRequestDTO;
import com.java_web.backend.Common.Entity.Course;
import com.java_web.backend.Common.Entity.CourseObjective;
import com.java_web.backend.Common.Entity.Syllabus;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.CourseObjectMapper;
import com.java_web.backend.Common.Mapper.SyllabusMapper;
import com.java_web.backend.Common.Service.LLMCallLogService;
import com.java_web.backend.Common.Service.LLMSyllabusService;
import com.java_web.backend.Common.Service.RestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SyllabusService {
    @Autowired
    private SyllabusMapper syllabusMapper;

    @Autowired
    private CourseObjectMapper courseObjectMapper;

    @Autowired
    private CourseService courseService;

    @Autowired
    private RestrictionService restrictionService;

    @Autowired
    private LLMSyllabusService llmService;

    @Autowired
    private ContentVersionService contentVersionService;

    @Autowired
    private LLMCallLogService llmCallLogService;

    public Syllabus getCourseSyllabus(Integer courseId, Integer teacherId) {
        courseService.validateApprovedCourse(courseId, teacherId);
        restrictionService.ensureFunctionAvailable(teacherId, "outline");
        return syllabusMapper.selectById(courseId);
    }

    public String generateSyllabusContent(Integer courseId, String prompt, Integer teacherId) {
        Course course = courseService.validateApprovedCourse(courseId, teacherId);
        restrictionService.ensureFunctionAvailable(teacherId, "outline");
        String requestSummary = llmCallLogService.summarizePrompt("syllabus_generation", prompt);

        CourseObjective objective = courseObjectMapper.selectById(courseId);
        if (objective == null
                || objective.getTeachingTarget() == null || objective.getTeachingTarget().trim().isEmpty()
                || objective.getCourseContent() == null || objective.getCourseContent().trim().isEmpty()) {
            throw ApiException.badRequest("请先完成课程介绍和教学目标");
        }

        try {
            SyllabusRequestDTO request = new SyllabusRequestDTO();
            request.setCourseTitle(course.getName());
            request.setRequest(prompt);
            String content = llmService.generateInitialSyllabus(request);
            llmCallLogService.recordSuccess(
                    teacherId,
                    courseId,
                    LLMCallLogService.MODULE_SYLLABUS,
                    requestSummary
            );
            return content;
        } catch (Exception exception) {
            llmCallLogService.recordFailure(
                    teacherId,
                    courseId,
                    LLMCallLogService.MODULE_SYLLABUS,
                    requestSummary,
                    exception
            );
            throw ApiException.badGateway("课程大纲生成失败，请稍后重试");
        }
    }

    public Syllabus saveSyllabus(Syllabus syllabus, Integer teacherId) {
        courseService.validateApprovedCourse(syllabus.getCourseId(), teacherId);
        restrictionService.ensureFunctionAvailable(teacherId, "outline");

        Syllabus existingSyllabus = syllabusMapper.selectById(syllabus.getCourseId());
        if (existingSyllabus == null) {
            syllabus.setCreatedAt(new Date());
            syllabus.setUpdatedAt(new Date());
            syllabusMapper.insert(syllabus);
            contentVersionService.recordSyllabusVersion(syllabus, teacherId);
            return syllabus;
        }

        existingSyllabus.setContent(syllabus.getContent());
        existingSyllabus.setUpdatedAt(new Date());
        syllabusMapper.updateById(existingSyllabus);
        contentVersionService.recordSyllabusVersion(existingSyllabus, teacherId);
        return existingSyllabus;
    }

    public String getCourseOutlineContent(Integer courseId) {
        Syllabus syllabus = syllabusMapper.selectById(courseId);
        return syllabus != null ? syllabus.getContent() : null;
    }
}
