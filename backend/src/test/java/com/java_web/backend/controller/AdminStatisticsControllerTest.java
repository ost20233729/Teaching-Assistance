package com.java_web.backend.controller;

import com.java_web.backend.Admin.Controller.AdminStatisticsController;
import com.java_web.backend.Admin.Service.AdminStatisticsService;
import com.java_web.backend.Common.DTO.LLMStatisticsDTO;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminStatisticsController.class)
@Import(AdminStatisticsController.class)
class AdminStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminStatisticsService adminStatisticsService;

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
    void getDashboardData_ShouldIncludeLlmStatistics() throws Exception {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("userStats", Map.of("totalUsers", 8, "teacherCount", 7, "adminCount", 1));
        dashboard.put("courseStats", Map.of("totalCourses", 14, "pendingCount", 3, "approvedCount", 10, "rejectedCount", 1));
        dashboard.put("llmStats", Map.of(
                "totalCalls", 18,
                "successCount", 15,
                "failedCount", 3,
                "successRate", 83.3
        ));
        when(adminStatisticsService.getDashboardData()).thenReturn(dashboard);

        mockMvc.perform(get("/api/v1/admin/statistics/dashboard")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.llmStats.totalCalls").value(18))
                .andExpect(jsonPath("$.llmStats.successCount").value(15));
    }

    @Test
    void getLlmStatistics_ShouldReturnDedicatedEndpointData() throws Exception {
        LLMStatisticsDTO response = new LLMStatisticsDTO(
                12,
                10,
                2,
                83.3,
                3,
                2,
                4,
                3,
                List.of()
        );
        when(adminStatisticsService.getLlmStatistics()).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/statistics/llm-calls")
                        .header("Authorization", "Bearer test-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalls").value(12))
                .andExpect(jsonPath("$.failedCount").value(2))
                .andExpect(jsonPath("$.markdownCount").value(3));
    }
}
