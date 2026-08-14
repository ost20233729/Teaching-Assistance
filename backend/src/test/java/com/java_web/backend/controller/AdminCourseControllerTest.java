package com.java_web.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_web.backend.Admin.Controller.AdminCourseController;
import com.java_web.backend.Admin.Service.AdminCourseService;
import com.java_web.backend.Common.DTO.CourseStatusUpdateDTO;
import com.java_web.backend.Common.DTO.PagedResponse;
import com.java_web.backend.Common.Entity.Course;
import com.java_web.backend.Common.Service.JWTService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCourseController.class)
@Import(AdminCourseController.class)
class AdminCourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminCourseService adminCourseService;

    @MockBean
    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        when(claims.get("role", String.class)).thenReturn("admin");
        when(claims.get("id")).thenReturn(1);
        when(claims.getSubject()).thenReturn("admin");
        when(jwtService.parseToken(anyString())).thenReturn(claims);
    }

    @Test
    void getCourses_ShouldReturnPagedResponse() throws Exception {
        Course course = new Course();
        course.setId(12);
        course.setTeacherId(2);
        course.setName("人工智能基础");
        course.setStatus("pending");
        course.setIsDeleted(0);

        PagedResponse<Course> response = new PagedResponse<>(List.of(course), 15, 1, 8, 2);
        when(adminCourseService.getCourses("pending", "人工", 1, 8)).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/courses")
                        .header("Authorization", "Bearer test-token")
                        .param("status", "pending")
                        .param("keyword", "人工")
                        .param("page", "1")
                        .param("pageSize", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("人工智能基础"))
                .andExpect(jsonPath("$.total").value(15))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(8))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void updateCourseStatus_ShouldReturnReviewCommentAndReviewedAt() throws Exception {
        CourseStatusUpdateDTO request = new CourseStatusUpdateDTO();
        request.setStatus("rejected");
        request.setReviewComment("请补充课程目标说明");

        Course course = new Course();
        course.setId(12);
        course.setTeacherId(2);
        course.setName("人工智能基础");
        course.setStatus("rejected");
        course.setReviewComment("请补充课程目标说明");
        course.setReviewedAt(new Date());
        course.setIsDeleted(0);

        when(adminCourseService.updateCourseStatus(12, "rejected", "请补充课程目标说明")).thenReturn(course);

        mockMvc.perform(patch("/api/v1/admin/courses/12")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.reviewComment").value("请补充课程目标说明"));
    }
}
