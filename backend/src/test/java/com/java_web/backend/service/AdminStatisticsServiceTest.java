package com.java_web.backend.service;

import com.java_web.backend.Admin.Service.AdminStatisticsService;
import com.java_web.backend.Common.DTO.LLMStatisticsDTO;
import com.java_web.backend.Common.Mapper.CourseMapper;
import com.java_web.backend.Common.Mapper.UserMapper;
import com.java_web.backend.Common.Service.LLMCallLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatisticsServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private LLMCallLogService llmCallLogService;

    @InjectMocks
    private AdminStatisticsService adminStatisticsService;

    @Test
    void getDashboardData_ShouldIncludeLlmStats() {
        LLMStatisticsDTO llmStatistics = new LLMStatisticsDTO(
                18,
                15,
                3,
                83.3,
                5,
                4,
                3,
                6,
                List.of()
        );

        when(userMapper.selectCount(any())).thenReturn(8L, 7L, 1L);
        when(courseMapper.selectCount(any())).thenReturn(14L, 3L, 10L, 1L);
        when(llmCallLogService.getStatistics()).thenReturn(llmStatistics);

        Map<String, Object> dashboard = adminStatisticsService.getDashboardData();

        assertTrue(dashboard.containsKey("userStats"));
        assertTrue(dashboard.containsKey("courseStats"));
        assertTrue(dashboard.containsKey("llmStats"));
        assertSame(llmStatistics, dashboard.get("llmStats"));
    }

    @Test
    void getLlmStatistics_ShouldDelegateToLogService() {
        LLMStatisticsDTO llmStatistics = new LLMStatisticsDTO(
                5,
                4,
                1,
                80.0,
                1,
                1,
                1,
                2,
                List.of()
        );
        when(llmCallLogService.getStatistics()).thenReturn(llmStatistics);

        LLMStatisticsDTO result = adminStatisticsService.getLlmStatistics();

        assertEquals(5, result.getTotalCalls());
        verify(llmCallLogService).getStatistics();
    }
}
