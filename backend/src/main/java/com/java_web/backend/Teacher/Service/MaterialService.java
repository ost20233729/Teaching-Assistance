package com.java_web.backend.Teacher.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_web.backend.Common.Entity.CourseObjective;
import com.java_web.backend.Common.Entity.InitialSyllabusRequest;
import com.java_web.backend.Common.Entity.Material;
import com.java_web.backend.Common.Entity.Syllabus;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.CourseObjectMapper;
import com.java_web.backend.Common.Mapper.MaterialMapper;
import com.java_web.backend.Common.Mapper.SyllabusMapper;
import com.java_web.backend.Common.Service.LLMCallLogService;
import com.java_web.backend.Common.Service.LLMInitialSyllabusService;
import com.java_web.backend.Common.Service.LLMLectureService;
import com.java_web.backend.Common.Service.RestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class MaterialService {
    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private CourseObjectMapper courseObjectMapper;

    @Autowired
    private SyllabusMapper syllabusMapper;

    @Autowired
    private CourseService courseService;

    @Autowired
    private RestrictionService restrictionService;

    @Autowired
    private LLMLectureService llmService;

    @Autowired
    private LLMInitialSyllabusService llmInitialSyllabusService;

    @Autowired
    private ContentVersionService contentVersionService;

    @Autowired
    private LLMCallLogService llmCallLogService;

    public Material getCourseMaterial(Integer courseId, Integer teacherId) {
        courseService.validateApprovedCourse(courseId, teacherId);
        restrictionService.ensureFunctionAvailable(teacherId, "lecture");
        return materialMapper.selectById(courseId);
    }

    public String generateMaterialContent(InitialSyllabusRequest req, Integer teacherId) throws JsonProcessingException {
        Integer courseId = Integer.valueOf(req.getCourseId());
        courseService.validateApprovedCourse(courseId, teacherId);
        restrictionService.ensureFunctionAvailable(teacherId, "lecture");
        String requestSummary = llmCallLogService.summarizePrompt("material_generation", req.getRequest());

        CourseObjective objective = courseObjectMapper.selectById(courseId);
        if (objective == null
                || objective.getCourseContent() == null || objective.getCourseContent().trim().isEmpty()
                || objective.getTeachingTarget() == null || objective.getTeachingTarget().trim().isEmpty()) {
            throw ApiException.badRequest("请先完成课程介绍和教学目标");
        }

        Syllabus syllabus = syllabusMapper.selectById(courseId);
        if (syllabus == null || syllabus.getContent() == null || syllabus.getContent().trim().isEmpty()) {
            throw ApiException.badRequest("请先完成课程大纲");
        }

        Map<String, Object> syllabusMap = null;
        for (int retryCount = 0; retryCount < 3 && syllabusMap == null; retryCount++) {
            syllabusMap = llmInitialSyllabusService.generateInitialSyllabus(
                    req.getCourseId(),
                    req.getCourseCode(),
                    req.getCourseTitle(),
                    req.getTeachingLanguage(),
                    req.getResponsibleCollege(),
                    req.getCourseCategory(),
                    req.getPrinciple(),
                    req.getVerifier(),
                    req.getCredit(),
                    req.getCourseHour(),
                    objective.getCourseContent(),
                    objective.getTeachingTarget(),
                    req.getEvaluationMode(),
                    req.getWhetherTechnicalCourse(),
                    req.getAssessmentType(),
                    req.getGradeRecording(),
                    req.getRequest()
            );
        }

        if (syllabusMap == null) {
            llmCallLogService.recordFailure(
                    teacherId,
                    courseId,
                    LLMCallLogService.MODULE_MATERIAL,
                    requestSummary,
                    new IllegalStateException("讲义生成结果为空")
            );
            throw ApiException.badGateway("讲义生成失败，请稍后重试");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            String rawJson = mapper.writeValueAsString(syllabusMap);
            String cleanedJson = rawJson.replaceAll("```json\\n?", "")
                    .replaceAll("\\n?```", "");
            JsonNode syllabusJson = mapper.readTree(cleanedJson);
            String content = llmService.generateLecture(syllabusJson);
            llmCallLogService.recordSuccess(
                    teacherId,
                    courseId,
                    LLMCallLogService.MODULE_MATERIAL,
                    requestSummary
            );
            return content;
        } catch (Exception exception) {
            llmCallLogService.recordFailure(
                    teacherId,
                    courseId,
                    LLMCallLogService.MODULE_MATERIAL,
                    requestSummary,
                    exception
            );
            throw exception;
        }
    }

    public Material saveMaterial(Material material, Integer teacherId) {
        courseService.validateApprovedCourse(material.getCourseId(), teacherId);
        restrictionService.ensureFunctionAvailable(teacherId, "lecture");

        String processedContent = material.getContent();
        if (processedContent != null) {
            processedContent = processedContent.replace("\\\\n", "\n")
                    .replace("\\\\t", "\t")
                    .replace("\\\\r", "\r");
            material.setContent(processedContent);
        }

        Material existingMaterial = materialMapper.selectById(material.getCourseId());
        if (existingMaterial == null) {
            material.setCreatedAt(new Date());
            material.setUpdatedAt(new Date());
            materialMapper.insert(material);
            contentVersionService.recordMaterialVersion(material, teacherId);
            return material;
        }

        existingMaterial.setContent(material.getContent());
        existingMaterial.setUpdatedAt(new Date());
        materialMapper.updateById(existingMaterial);
        contentVersionService.recordMaterialVersion(existingMaterial, teacherId);
        return existingMaterial;
    }
}
