package com.java_web.backend.controller;

import com.java_web.backend.Common.Entity.Courseware;
import com.java_web.backend.Common.Service.JWTService;
import com.java_web.backend.Teacher.Controller.CoursewareController;
import com.java_web.backend.Teacher.Service.CoursewareService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoursewareController.class)
@Import(CoursewareController.class)
class CoursewareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoursewareService coursewareService;

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
    void getCourseware_ShouldReturnSavedContent() throws Exception {
        Courseware courseware = new Courseware();
        courseware.setCourseId(12);
        courseware.setContent("# 教学课件");

        when(coursewareService.getCourseware(12, 2)).thenReturn(courseware);

        mockMvc.perform(get("/api/v1/teacher/courses/12/courseware")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(12))
                .andExpect(jsonPath("$.content").value("# 教学课件"));
    }

    @Test
    void generateCourseware_ShouldReturnCreatedMarkdown() throws Exception {
        when(coursewareService.generateCoursewareContent(12, "生成课件提纲", 2))
                .thenReturn("# 教学课件提纲");

        mockMvc.perform(post("/api/v1/teacher/courses/12/courseware-generations")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"生成课件提纲\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("# 教学课件提纲"));
    }
}
