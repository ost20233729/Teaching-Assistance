package com.java_web.backend.controller;

import com.java_web.backend.Common.DTO.ContentVersionDTO;
import com.java_web.backend.Common.DTO.ContentVersionRestoreResponseDTO;
import com.java_web.backend.Common.Service.JWTService;
import com.java_web.backend.Teacher.Controller.ContentVersionController;
import com.java_web.backend.Teacher.Service.ContentVersionService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContentVersionController.class)
@Import(ContentVersionController.class)
class ContentVersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContentVersionService contentVersionService;

    @MockBean
    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        when(claims.get("role", String.class)).thenReturn("teacher");
        when(claims.get("id")).thenReturn(2);
        when(claims.getSubject()).thenReturn("teacher");
        when(jwtService.parseToken(anyString())).thenReturn(claims);
    }

    @Test
    void getVersions_ShouldReturnVersionList() throws Exception {
        ContentVersionDTO version = new ContentVersionDTO(
                9L,
                12,
                "objective",
                "课程介绍预览",
                "## 课程介绍\n\n内容",
                2,
                new Date()
        );

        when(contentVersionService.listVersions(12, "objective", 2)).thenReturn(List.of(version));

        mockMvc.perform(get("/api/v1/teacher/courses/12/content-versions")
                        .header("Authorization", "Bearer test-token")
                        .param("module", "objective"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].moduleType").value("objective"))
                .andExpect(jsonPath("$[0].preview").value("课程介绍预览"));
    }

    @Test
    void restoreVersion_ShouldReturnRestoredPayload() throws Exception {
        ContentVersionRestoreResponseDTO response = new ContentVersionRestoreResponseDTO(
                "syllabus",
                Map.of("content", "# 恢复后的课程大纲", "courseId", 12)
        );

        when(contentVersionService.restoreVersion(12, 6L, 2)).thenReturn(response);

        mockMvc.perform(post("/api/v1/teacher/courses/12/content-versions/6/restorations")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleType").value("syllabus"))
                .andExpect(jsonPath("$.data.content").value("# 恢复后的课程大纲"));
    }
}
