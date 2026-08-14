package com.java_web.backend.Admin.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.java_web.backend.Common.DTO.LLMStatisticsDTO;
import com.java_web.backend.Common.Entity.Course;
import com.java_web.backend.Common.Entity.User;
import com.java_web.backend.Common.Mapper.CourseMapper;
import com.java_web.backend.Common.Mapper.UserMapper;
import com.java_web.backend.Common.Service.LLMCallLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminStatisticsService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private LLMCallLogService llmCallLogService;

    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();

        QueryWrapper<User> totalQuery = new QueryWrapper<>();
        totalQuery.eq("is_deleted", 0);
        Long totalUsers = userMapper.selectCount(totalQuery);

        QueryWrapper<User> teacherQuery = new QueryWrapper<>();
        teacherQuery.eq("role", "teacher").eq("is_deleted", 0);
        Long teacherCount = userMapper.selectCount(teacherQuery);

        QueryWrapper<User> adminQuery = new QueryWrapper<>();
        adminQuery.eq("role", "admin").eq("is_deleted", 0);
        Long adminCount = userMapper.selectCount(adminQuery);

        stats.put("totalUsers", totalUsers);
        stats.put("teacherCount", teacherCount);
        stats.put("adminCount", adminCount);
        return stats;
    }

    public Map<String, Object> getCourseStatistics() {
        Map<String, Object> stats = new HashMap<>();

        QueryWrapper<Course> totalQuery = new QueryWrapper<>();
        totalQuery.eq("is_deleted", 0);
        Long totalCourses = courseMapper.selectCount(totalQuery);

        QueryWrapper<Course> pendingQuery = new QueryWrapper<>();
        pendingQuery.eq("status", "pending").eq("is_deleted", 0);
        Long pendingCount = courseMapper.selectCount(pendingQuery);

        QueryWrapper<Course> approvedQuery = new QueryWrapper<>();
        approvedQuery.eq("status", "approved").eq("is_deleted", 0);
        Long approvedCount = courseMapper.selectCount(approvedQuery);

        QueryWrapper<Course> rejectedQuery = new QueryWrapper<>();
        rejectedQuery.eq("status", "rejected").eq("is_deleted", 0);
        Long rejectedCount = courseMapper.selectCount(rejectedQuery);

        stats.put("totalCourses", totalCourses);
        stats.put("pendingCount", pendingCount);
        stats.put("approvedCount", approvedCount);
        stats.put("rejectedCount", rejectedCount);
        return stats;
    }

    public LLMStatisticsDTO getLlmStatistics() {
        return llmCallLogService.getStatistics();
    }

    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("userStats", getUserStatistics());
        dashboard.put("courseStats", getCourseStatistics());
        dashboard.put("llmStats", getLlmStatistics());
        return dashboard;
    }
}
