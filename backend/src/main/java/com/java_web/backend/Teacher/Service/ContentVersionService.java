package com.java_web.backend.Teacher.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_web.backend.Common.DTO.ContentVersionDTO;
import com.java_web.backend.Common.DTO.ContentVersionRestoreResponseDTO;
import com.java_web.backend.Common.Entity.CourseContentVersion;
import com.java_web.backend.Common.Entity.CourseObjective;
import com.java_web.backend.Common.Entity.Courseware;
import com.java_web.backend.Common.Entity.Material;
import com.java_web.backend.Common.Entity.Syllabus;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.CourseContentVersionMapper;
import com.java_web.backend.Common.Mapper.CourseObjectMapper;
import com.java_web.backend.Common.Mapper.CoursewareMapper;
import com.java_web.backend.Common.Mapper.MaterialMapper;
import com.java_web.backend.Common.Mapper.SyllabusMapper;
import com.java_web.backend.Common.Service.RestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContentVersionService {
    private static final String MODULE_OBJECTIVE = "objective";
    private static final String MODULE_SYLLABUS = "syllabus";
    private static final String MODULE_MATERIAL = "material";
    private static final String MODULE_COURSEWARE = "courseware";
    private static final int PREVIEW_LIMIT = 120;

    @Autowired
    private CourseContentVersionMapper versionMapper;

    @Autowired
    private CourseObjectMapper courseObjectMapper;

    @Autowired
    private SyllabusMapper syllabusMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private CoursewareMapper coursewareMapper;

    @Autowired
    private CourseService courseService;

    @Autowired
    private RestrictionService restrictionService;

    @Autowired
    private ObjectMapper objectMapper;

    public List<ContentVersionDTO> listVersions(Integer courseId, String moduleType, Integer teacherId) {
        String normalizedModuleType = normalizeModuleType(moduleType);
        ensureModuleAccess(courseId, teacherId, normalizedModuleType);

        LambdaQueryWrapper<CourseContentVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseContentVersion::getCourseId, courseId)
                .eq(CourseContentVersion::getModuleType, normalizedModuleType)
                .orderByDesc(CourseContentVersion::getCreatedAt)
                .orderByDesc(CourseContentVersion::getId);

        List<CourseContentVersion> versions = versionMapper.selectList(queryWrapper);
        List<ContentVersionDTO> result = new ArrayList<>();
        for (CourseContentVersion version : versions) {
            result.add(toDto(version));
        }
        return result;
    }

    public ContentVersionRestoreResponseDTO restoreVersion(Integer courseId, Long versionId, Integer teacherId) {
        CourseContentVersion version = versionMapper.selectById(versionId);
        if (version == null || !courseId.equals(version.getCourseId())) {
            throw ApiException.notFound("历史版本不存在");
        }

        String moduleType = normalizeModuleType(version.getModuleType());
        ensureModuleAccess(courseId, teacherId, moduleType);

        return switch (moduleType) {
            case MODULE_OBJECTIVE -> restoreObjectiveVersion(version, teacherId);
            case MODULE_SYLLABUS -> restoreSyllabusVersion(version, teacherId);
            case MODULE_MATERIAL -> restoreMaterialVersion(version, teacherId);
            case MODULE_COURSEWARE -> restoreCoursewareVersion(version, teacherId);
            default -> throw ApiException.badRequest("不支持的版本模块类型");
        };
    }

    public void recordObjectiveVersion(CourseObjective objective, Integer teacherId) {
        saveVersionSnapshot(
                objective.getCourseId(),
                MODULE_OBJECTIVE,
                serializeObjectiveSnapshot(objective),
                teacherId
        );
    }

    public void recordSyllabusVersion(Syllabus syllabus, Integer teacherId) {
        saveVersionSnapshot(
                syllabus.getCourseId(),
                MODULE_SYLLABUS,
                valueOrEmpty(syllabus.getContent()),
                teacherId
        );
    }

    public void recordMaterialVersion(Material material, Integer teacherId) {
        saveVersionSnapshot(
                material.getCourseId(),
                MODULE_MATERIAL,
                valueOrEmpty(material.getContent()),
                teacherId
        );
    }

    public void recordCoursewareVersion(Courseware courseware, Integer teacherId) {
        saveVersionSnapshot(
                courseware.getCourseId(),
                MODULE_COURSEWARE,
                valueOrEmpty(courseware.getContent()),
                teacherId
        );
    }

    private ContentVersionRestoreResponseDTO restoreObjectiveVersion(CourseContentVersion version, Integer teacherId) {
        Map<String, String> snapshot = readObjectiveSnapshot(version.getContent());
        CourseObjective objective = courseObjectMapper.selectById(version.getCourseId());
        Date now = new Date();

        if (objective == null) {
            objective = new CourseObjective();
            objective.setCourseId(version.getCourseId());
            objective.setCreatedAt(now);
            objective.setUpdatedAt(now);
            objective.setCourseContent(snapshot.get("courseContent"));
            objective.setTeachingTarget(snapshot.get("teachingTarget"));
            courseObjectMapper.insert(objective);
        } else {
            objective.setCourseContent(snapshot.get("courseContent"));
            objective.setTeachingTarget(snapshot.get("teachingTarget"));
            objective.setUpdatedAt(now);
            courseObjectMapper.updateById(objective);
        }

        recordObjectiveVersion(objective, teacherId);

        Map<String, Object> data = new HashMap<>();
        data.put("courseContent", objective.getCourseContent());
        data.put("teachingTarget", objective.getTeachingTarget());
        data.put("courseId", objective.getCourseId());
        return new ContentVersionRestoreResponseDTO(MODULE_OBJECTIVE, data);
    }

    private ContentVersionRestoreResponseDTO restoreSyllabusVersion(CourseContentVersion version, Integer teacherId) {
        Syllabus syllabus = syllabusMapper.selectById(version.getCourseId());
        Date now = new Date();

        if (syllabus == null) {
            syllabus = new Syllabus();
            syllabus.setCourseId(version.getCourseId());
            syllabus.setCreatedAt(now);
            syllabus.setUpdatedAt(now);
            syllabus.setContent(version.getContent());
            syllabusMapper.insert(syllabus);
        } else {
            syllabus.setContent(version.getContent());
            syllabus.setUpdatedAt(now);
            syllabusMapper.updateById(syllabus);
        }

        recordSyllabusVersion(syllabus, teacherId);

        Map<String, Object> data = new HashMap<>();
        data.put("content", syllabus.getContent());
        data.put("courseId", syllabus.getCourseId());
        return new ContentVersionRestoreResponseDTO(MODULE_SYLLABUS, data);
    }

    private ContentVersionRestoreResponseDTO restoreMaterialVersion(CourseContentVersion version, Integer teacherId) {
        Material material = materialMapper.selectById(version.getCourseId());
        Date now = new Date();

        if (material == null) {
            material = new Material();
            material.setCourseId(version.getCourseId());
            material.setCreatedAt(now);
            material.setUpdatedAt(now);
            material.setContent(version.getContent());
            materialMapper.insert(material);
        } else {
            material.setContent(version.getContent());
            material.setUpdatedAt(now);
            materialMapper.updateById(material);
        }

        recordMaterialVersion(material, teacherId);

        Map<String, Object> data = new HashMap<>();
        data.put("content", material.getContent());
        data.put("courseId", material.getCourseId());
        return new ContentVersionRestoreResponseDTO(MODULE_MATERIAL, data);
    }

    private ContentVersionRestoreResponseDTO restoreCoursewareVersion(CourseContentVersion version, Integer teacherId) {
        Courseware courseware = coursewareMapper.selectById(version.getCourseId());
        Date now = new Date();

        if (courseware == null) {
            courseware = new Courseware();
            courseware.setCourseId(version.getCourseId());
            courseware.setCreatedAt(now);
            courseware.setUpdatedAt(now);
            courseware.setContent(version.getContent());
            coursewareMapper.insert(courseware);
        } else {
            courseware.setContent(version.getContent());
            courseware.setUpdatedAt(now);
            coursewareMapper.updateById(courseware);
        }

        recordCoursewareVersion(courseware, teacherId);

        Map<String, Object> data = new HashMap<>();
        data.put("content", courseware.getContent());
        data.put("courseId", courseware.getCourseId());
        return new ContentVersionRestoreResponseDTO(MODULE_COURSEWARE, data);
    }

    private void saveVersionSnapshot(Integer courseId, String moduleType, String content, Integer teacherId) {
        CourseContentVersion latestVersion = findLatestVersion(courseId, moduleType);
        if (latestVersion != null && valueOrEmpty(latestVersion.getContent()).equals(valueOrEmpty(content))) {
            return;
        }

        CourseContentVersion version = new CourseContentVersion();
        version.setCourseId(courseId);
        version.setModuleType(moduleType);
        version.setContent(content);
        version.setCreatedBy(teacherId);
        version.setCreatedAt(new Date());
        versionMapper.insert(version);
    }

    private CourseContentVersion findLatestVersion(Integer courseId, String moduleType) {
        LambdaQueryWrapper<CourseContentVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CourseContentVersion::getCourseId, courseId)
                .eq(CourseContentVersion::getModuleType, moduleType)
                .orderByDesc(CourseContentVersion::getCreatedAt)
                .orderByDesc(CourseContentVersion::getId)
                .last("LIMIT 1");
        return versionMapper.selectOne(queryWrapper);
    }

    private ContentVersionDTO toDto(CourseContentVersion version) {
        String displayContent = switch (version.getModuleType()) {
            case MODULE_OBJECTIVE -> formatObjectiveDisplayContent(version.getContent());
            case MODULE_SYLLABUS, MODULE_MATERIAL, MODULE_COURSEWARE -> valueOrEmpty(version.getContent());
            default -> valueOrEmpty(version.getContent());
        };

        return new ContentVersionDTO(
                version.getId(),
                version.getCourseId(),
                version.getModuleType(),
                buildPreview(displayContent),
                displayContent,
                version.getCreatedBy(),
                version.getCreatedAt()
        );
    }

    private String serializeObjectiveSnapshot(CourseObjective objective) {
        Map<String, String> snapshot = new HashMap<>();
        snapshot.put("courseContent", valueOrEmpty(objective.getCourseContent()));
        snapshot.put("teachingTarget", valueOrEmpty(objective.getTeachingTarget()));

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw ApiException.badGateway("版本快照保存失败");
        }
    }

    private Map<String, String> readObjectiveSnapshot(String content) {
        try {
            return objectMapper.readValue(valueOrEmpty(content), new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException exception) {
            throw ApiException.badGateway("历史版本内容解析失败");
        }
    }

    private String formatObjectiveDisplayContent(String content) {
        Map<String, String> snapshot = readObjectiveSnapshot(content);
        return "## 课程介绍\n\n" +
                valueOrFallback(snapshot.get("courseContent")) +
                "\n\n## 教学目标\n\n" +
                valueOrFallback(snapshot.get("teachingTarget"));
    }

    private String buildPreview(String content) {
        String normalized = valueOrEmpty(content)
                .replaceAll("#+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isEmpty()) {
            return "当前版本内容为空";
        }

        if (normalized.length() <= PREVIEW_LIMIT) {
            return normalized;
        }

        return normalized.substring(0, PREVIEW_LIMIT) + "...";
    }

    private void ensureModuleAccess(Integer courseId, Integer teacherId, String moduleType) {
        courseService.validateApprovedCourse(courseId, teacherId);
        String restrictionName = switch (moduleType) {
            case MODULE_OBJECTIVE -> "basic";
            case MODULE_SYLLABUS -> "outline";
            case MODULE_MATERIAL -> "lecture";
            case MODULE_COURSEWARE -> "courseware";
            default -> throw ApiException.badRequest("不支持的版本模块类型");
        };
        restrictionService.ensureFunctionAvailable(teacherId, restrictionName);
    }

    private String normalizeModuleType(String moduleType) {
        if (moduleType == null) {
            throw ApiException.badRequest("缺少 module 参数");
        }

        String normalized = moduleType.trim();
        if (!MODULE_OBJECTIVE.equals(normalized) &&
                !MODULE_SYLLABUS.equals(normalized) &&
                !MODULE_MATERIAL.equals(normalized) &&
                !MODULE_COURSEWARE.equals(normalized)) {
            throw ApiException.badRequest("不支持的模块类型");
        }

        return normalized;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String valueOrFallback(String value) {
        String normalized = valueOrEmpty(value).trim();
        return normalized.isEmpty() ? "当前版本内容为空" : normalized;
    }
}
