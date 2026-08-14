package com.java_web.backend.controller;

import com.java_web.backend.Common.DTO.CourseExportDTO;
import com.java_web.backend.Common.DTO.PagedResponse;
import com.java_web.backend.Common.Entity.Course;
import com.java_web.backend.Common.Service.JWTService;
import com.java_web.backend.Teacher.Controller.CourseController;
import com.java_web.backend.Teacher.Service.CourseService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@Import(CourseController.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @MockBean
    private JWTService jwtService;

    @Mock
    private Claims claims;

    @BeforeEach
    void setUp() {
        claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.get("role", String.class)).thenReturn("teacher");
        when(claims.get("id")).thenReturn(1);
        when(claims.getSubject()).thenReturn("tester");
        when(jwtService.parseToken(anyString())).thenReturn(claims);
    }

    @Test
    void exportCourseMarkdown_Success() throws Exception {
        CourseExportDTO export = new CourseExportDTO("人工智能基础-课程成果.md", "# 人工智能基础 课程成果");
        when(courseService.exportCourseMarkdown(12, 1)).thenReturn(export);

        mockMvc.perform(get("/api/v1/teacher/courses/12/export/markdown")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(content().contentType("text/markdown;charset=UTF-8"))
                .andExpect(content().string("# 人工智能基础 课程成果"));
    }

    @Test
    void getCoursesByTeacher_ShouldReturnPagedResponse() throws Exception {
        Course course = new Course();
        course.setId(12);
        course.setTeacherId(1);
        course.setName("人工智能基础");
        course.setStatus("approved");
        course.setIsDeleted(0);

        PagedResponse<Course> response = new PagedResponse<>(List.of(course), 9, 2, 8, 2);
        when(courseService.getTeacherCourses(1, "人工", "approved", 2, 8)).thenReturn(response);

        mockMvc.perform(get("/api/v1/teacher/courses")
                        .header("Authorization", "Bearer test-token")
                        .param("keyword", "人工")
                        .param("status", "approved")
                        .param("page", "2")
                        .param("pageSize", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("人工智能基础"))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(8))
                .andExpect(jsonPath("$.total").value(9))
                .andExpect(jsonPath("$.totalPages").value(2));
    }
}
