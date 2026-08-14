package com.java_web.backend.Teacher.Service;

import com.java_web.backend.Common.DTO.IntroductionAndTargetRequestDTO;
import com.java_web.backend.Common.Entity.Course;
import com.java_web.backend.Common.Entity.CourseObjective;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.CourseObjectMapper;
import com.java_web.backend.Common.Service.LLMCallLogService;
import com.java_web.backend.Common.Service.LLMIntroductionAndTargetService;
import com.java_web.backend.Common.Service.RestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class CourseObjectiveService {
    @Autowired
    private CourseObjectMapper courseObjectMapper;

    @Autowired
    private CourseService courseService;

    @Autowired
    private RestrictionService restrictionService;

    @Autowired
    private LLMIntroductionAndTargetService llmService;

    @Autowired
    private ContentVersionService contentVersionService;

    @Autowired
    private LLMCallLogService llmCallLogService;

    public CourseObjective getCourseObjective(Integer courseId, Integer teacherId) {
        courseService.validateApprovedCourse(courseId, teacherId);
        restrictionService.ensureFunctionAvailable(teacherId, "basic");
        return courseObjectMapper.selectById(courseId);
    }

    public Map<String, String> generateObjectiveContent(Integer courseId, String prompt, Integer teacherId) throws IOException {
        Course course = courseService.validateApprovedCourse(courseId, teacherId);
        restrictionService.ensureFunctionAvailable(teacherId, "basic");
        String requestSummary = llmCallLogService.summarizePrompt("objective_generation", prompt);

        IntroductionAndTargetRequestDTO request = new IntroductionAndTargetRequestDTO();
        request.setCourseTitle(course.getName());
        request.setRequest(prompt);

        try {
            var response = llmService.generateIntroductionAndTarget(request);
            llmCallLogService.recordSuccess(
                    teacherId,
                    courseId,
                    LLMCallLogService.MODULE_OBJECTIVE,
                    requestSummary
            );
            Map<String, String> result = new HashMap<>();
            result.put("courseContent", response.getCourseIntroduction());
            result.put("teachingTarget", response.getTeachingTarget());
            return result;
        } catch (Exception exception) {
            llmCallLogService.recordFailure(
                    teacherId,
                    courseId,
                    LLMCallLogService.MODULE_OBJECTIVE,
                    requestSummary,
                    exception
            );
            throw ApiException.badGateway("璇剧▼浠嬬粛鍜屾暀瀛︾洰鏍囩敓鎴愬け璐ワ紝璇风◢鍚庨噸璇?");
        }
    }

    public CourseObjective saveObjective(CourseObjective objective, Integer teacherId) {
        courseService.validateApprovedCourse(objective.getCourseId(), teacherId);
        restrictionService.ensureFunctionAvailable(teacherId, "basic");

        CourseObjective existingObj = courseObjectMapper.selectById(objective.getCourseId());
        if (existingObj == null) {
            objective.setCreatedAt(new Date());
            objective.setUpdatedAt(new Date());
            courseObjectMapper.insert(objective);
            contentVersionService.recordObjectiveVersion(objective, teacherId);
            return objective;
        }

        existingObj.setCourseContent(objective.getCourseContent());
        existingObj.setTeachingTarget(objective.getTeachingTarget());
        existingObj.setUpdatedAt(new Date());
        courseObjectMapper.updateById(existingObj);
        contentVersionService.recordObjectiveVersion(existingObj, teacherId);
        return existingObj;
    }
}
