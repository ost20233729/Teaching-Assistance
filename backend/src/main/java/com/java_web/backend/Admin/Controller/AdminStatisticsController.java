package com.java_web.backend.Admin.Controller;

import com.java_web.backend.Admin.Service.AdminStatisticsService;
import com.java_web.backend.Common.DTO.LLMStatisticsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/statistics")
public class AdminStatisticsController {
    @Autowired
    private AdminStatisticsService adminStatisticsService;

    @GetMapping("/users")
    public Map<String, Object> getUserStatistics() {
        return adminStatisticsService.getUserStatistics();
    }

    @GetMapping("/courses")
    public Map<String, Object> getCourseStatistics() {
        return adminStatisticsService.getCourseStatistics();
    }

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        return adminStatisticsService.getDashboardData();
    }

    @GetMapping("/llm-calls")
    public LLMStatisticsDTO getLlmStatistics() {
        return adminStatisticsService.getLlmStatistics();
    }
}
