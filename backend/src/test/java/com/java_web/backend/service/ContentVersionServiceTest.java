package com.java_web.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_web.backend.Common.DTO.ContentVersionDTO;
import com.java_web.backend.Common.DTO.ContentVersionRestoreResponseDTO;
import com.java_web.backend.Common.Entity.CourseContentVersion;
import com.java_web.backend.Common.Entity.CourseObjective;
import com.java_web.backend.Common.Entity.Courseware;
import com.java_web.backend.Common.Entity.Material;
import com.java_web.backend.Common.Entity.Syllabus;
import com.java_web.backend.Common.Mapper.CourseContentVersionMapper;
import com.java_web.backend.Common.Mapper.CourseObjectMapper;
import com.java_web.backend.Common.Mapper.CoursewareMapper;
import com.java_web.backend.Common.Mapper.MaterialMapper;
import com.java_web.backend.Common.Mapper.SyllabusMapper;
import com.java_web.backend.Common.Service.RestrictionService;
import com.java_web.backend.Teacher.Service.ContentVersionService;
import com.java_web.backend.Teacher.Service.CourseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentVersionServiceTest {

    @Mock
    private CourseContentVersionMapper versionMapper;

    @Mock
    private CourseObjectMapper courseObjectMapper;

    @Mock
    private SyllabusMapper syllabusMapper;

    @Mock
    private MaterialMapper materialMapper;

    @Mock
    private CoursewareMapper coursewareMapper;

    @Mock
    private CourseService courseService;

    @Mock
    private RestrictionService restrictionService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ContentVersionService contentVersionService;

    @Test
    void recordObjectiveVersion_ShouldInsertSnapshotWhenContentChanges() {
        CourseObjective objective = new CourseObjective();
        objective.setCourseId(12);
        objective.setCourseContent("课程介绍");
        objective.setTeachingTarget("教学目标");

        when(versionMapper.selectOne(any())).thenReturn(null);

        contentVersionService.recordObjectiveVersion(objective, 2);

        verify(versionMapper).insert(any(CourseContentVersion.class));
    }

    @Test
    void recordSyllabusVersion_ShouldSkipDuplicateSnapshot() {
        Syllabus syllabus = new Syllabus();
        syllabus.setCourseId(12);
        syllabus.setContent("# 第1周");

        CourseContentVersion existingVersion = new CourseContentVersion();
        existingVersion.setCourseId(12);
        existingVersion.setModuleType("syllabus");
        existingVersion.setContent("# 第1周");

        when(versionMapper.selectOne(any())).thenReturn(existingVersion);

        contentVersionService.recordSyllabusVersion(syllabus, 2);

        verify(versionMapper, never()).insert(any(CourseContentVersion.class));
    }

    @Test
    void listVersions_ShouldFormatObjectiveSnapshot() {
        CourseContentVersion version = new CourseContentVersion();
        version.setId(8L);
        version.setCourseId(12);
        version.setModuleType("objective");
        version.setContent("{\"courseContent\":\"课程介绍内容\",\"teachingTarget\":\"教学目标内容\"}");
        version.setCreatedBy(2);
        version.setCreatedAt(new Date());

        when(versionMapper.selectList(any())).thenReturn(List.of(version));

        List<ContentVersionDTO> response = contentVersionService.listVersions(12, "objective", 2);

        assertEquals(1, response.size());
        assertTrue(response.get(0).getContent().contains("## 课程介绍"));
        assertTrue(response.get(0).getContent().contains("教学目标内容"));
        verify(courseService).validateApprovedCourse(12, 2);
        verify(restrictionService).ensureFunctionAvailable(2, "basic");
    }

    @Test
    void restoreVersion_ShouldWriteObjectiveSnapshotBackToCurrentContent() {
        CourseContentVersion version = new CourseContentVersion();
        version.setId(5L);
        version.setCourseId(12);
        version.setModuleType("objective");
        version.setContent("{\"courseContent\":\"恢复后的课程介绍\",\"teachingTarget\":\"恢复后的教学目标\"}");

        CourseObjective existingObjective = new CourseObjective();
        existingObjective.setCourseId(12);
        existingObjective.setCourseContent("旧内容");
        existingObjective.setTeachingTarget("旧目标");

        when(versionMapper.selectById(5L)).thenReturn(version);
        when(courseObjectMapper.selectById(12)).thenReturn(existingObjective);
        when(versionMapper.selectOne(any())).thenReturn(null);

        ContentVersionRestoreResponseDTO response = contentVersionService.restoreVersion(12, 5L, 2);

        assertEquals("objective", response.getModuleType());
        assertEquals("恢复后的课程介绍", response.getData().get("courseContent"));
        assertEquals("恢复后的教学目标", response.getData().get("teachingTarget"));
        verify(courseObjectMapper).updateById(existingObjective);
        verify(versionMapper).insert(any(CourseContentVersion.class));
    }

    @Test
    void restoreVersion_ShouldWriteMaterialSnapshotBackToCurrentContent() {
        CourseContentVersion version = new CourseContentVersion();
        version.setId(7L);
        version.setCourseId(15);
        version.setModuleType("material");
        version.setContent("# 恢复后的讲义");

        Material existingMaterial = new Material();
        existingMaterial.setCourseId(15);
        existingMaterial.setContent("# 旧讲义");

        when(versionMapper.selectById(7L)).thenReturn(version);
        when(materialMapper.selectById(15)).thenReturn(existingMaterial);
        when(versionMapper.selectOne(any())).thenReturn(null);

        ContentVersionRestoreResponseDTO response = contentVersionService.restoreVersion(15, 7L, 3);

        assertEquals("material", response.getModuleType());
        assertEquals("# 恢复后的讲义", response.getData().get("content"));
        verify(materialMapper).updateById(existingMaterial);
        verify(versionMapper).insert(any(CourseContentVersion.class));
    }

    @Test
    void recordCoursewareVersion_ShouldInsertSnapshotWhenContentChanges() {
        Courseware courseware = new Courseware();
        courseware.setCourseId(18);
        courseware.setContent("# 课件提纲");

        when(versionMapper.selectOne(any())).thenReturn(null);

        contentVersionService.recordCoursewareVersion(courseware, 4);

        verify(versionMapper).insert(any(CourseContentVersion.class));
    }

    @Test
    void listVersions_ShouldAllowCoursewareModule() {
        CourseContentVersion version = new CourseContentVersion();
        version.setId(11L);
        version.setCourseId(18);
        version.setModuleType("courseware");
        version.setContent("# 课件提纲");
        version.setCreatedBy(4);
        version.setCreatedAt(new Date());

        when(versionMapper.selectList(any())).thenReturn(List.of(version));

        List<ContentVersionDTO> response = contentVersionService.listVersions(18, "courseware", 4);

        assertEquals(1, response.size());
        assertEquals("courseware", response.get(0).getModuleType());
        assertEquals("# 课件提纲", response.get(0).getContent());
        verify(courseService).validateApprovedCourse(18, 4);
        verify(restrictionService).ensureFunctionAvailable(4, "courseware");
    }

    @Test
    void restoreVersion_ShouldWriteCoursewareSnapshotBackToCurrentContent() {
        CourseContentVersion version = new CourseContentVersion();
        version.setId(12L);
        version.setCourseId(18);
        version.setModuleType("courseware");
        version.setContent("# 恢复后的课件提纲");

        Courseware existingCourseware = new Courseware();
        existingCourseware.setCourseId(18);
        existingCourseware.setContent("# 旧课件提纲");

        when(versionMapper.selectById(12L)).thenReturn(version);
        when(coursewareMapper.selectById(18)).thenReturn(existingCourseware);
        when(versionMapper.selectOne(any())).thenReturn(null);

        ContentVersionRestoreResponseDTO response = contentVersionService.restoreVersion(18, 12L, 4);

        assertEquals("courseware", response.getModuleType());
        assertEquals("# 恢复后的课件提纲", response.getData().get("content"));
        verify(coursewareMapper).updateById(existingCourseware);
        verify(versionMapper).insert(any(CourseContentVersion.class));
    }
}
