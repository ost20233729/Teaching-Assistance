package com.java_web.backend.Common.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.java_web.backend.Common.DTO.LLMCallLogDTO;
import com.java_web.backend.Common.DTO.LLMStatisticsDTO;
import com.java_web.backend.Common.Entity.LLMCallLog;
import com.java_web.backend.Common.Entity.User;
import com.java_web.backend.Common.Mapper.LLMCallLogMapper;
import com.java_web.backend.Common.Mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class LLMCallLogService {
    public static final String MODULE_OBJECTIVE = "objective";
    public static final String MODULE_SYLLABUS = "syllabus";
    public static final String MODULE_MATERIAL = "material";
    public static final String MODULE_COURSEWARE = "courseware";
    public static final String MODULE_MARKDOWN_CONVERSION = "markdown_conversion";
    public static final String MODULE_MARKDOWN_BATCH = "markdown_conversion_batch";

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";
    private static final int RECENT_LOG_LIMIT = 10;
    private static final int MAX_SUMMARY_LENGTH = 240;
    private static final int MAX_ERROR_LENGTH = 500;

    @Autowired
    private LLMCallLogMapper llmCallLogMapper;

    @Autowired
    private UserMapper userMapper;

    public void recordSuccess(Integer userId, Integer courseId, String moduleType, String requestSummary) {
        saveLog(userId, courseId, moduleType, requestSummary, STATUS_SUCCESS, null);
    }

    public void recordFailure(Integer userId,
                              Integer courseId,
                              String moduleType,
                              String requestSummary,
                              Throwable throwable) {
        saveLog(userId, courseId, moduleType, requestSummary, STATUS_FAILED, resolveErrorMessage(throwable));
    }

    public String summarizePrompt(String label, String value) {
        String normalizedLabel = normalizeText(label);
        String normalizedValue = normalizeText(value);
        if (normalizedValue.isEmpty()) {
            return normalizedLabel + "为空";
        }
        return truncate(normalizedLabel + ": " + normalizedValue, MAX_SUMMARY_LENGTH);
    }

    public String summarizeMarkdownRequest(String outputFormat, String customStyle, int contentLength) {
        String resolvedFormat = normalizeText(outputFormat).isEmpty() ? "markdown" : outputFormat.trim();
        String styleSummary = normalizeText(customStyle).isEmpty() ? "默认样式" : truncate(normalizeText(customStyle), 80);
        return truncate(
                String.format("outputFormat=%s, customStyle=%s, jsonLength=%d", resolvedFormat, styleSummary, contentLength),
                MAX_SUMMARY_LENGTH
        );
    }

    public String summarizeMarkdownBatchRequest(String outputFormat, String customStyle, int itemCount) {
        String resolvedFormat = normalizeText(outputFormat).isEmpty() ? "markdown" : outputFormat.trim();
        String styleSummary = normalizeText(customStyle).isEmpty() ? "默认样式" : truncate(normalizeText(customStyle), 80);
        return truncate(
                String.format("batchSize=%d, outputFormat=%s, customStyle=%s", itemCount, resolvedFormat, styleSummary),
                MAX_SUMMARY_LENGTH
        );
    }

    public LLMStatisticsDTO getStatistics() {
        long totalCalls = countBy(null, null);
        long successCount = countBy(STATUS_SUCCESS, null);
        long failedCount = countBy(STATUS_FAILED, null);
        long objectiveCount = countBy(null, MODULE_OBJECTIVE);
        long syllabusCount = countBy(null, MODULE_SYLLABUS);
        long materialCount = countBy(null, MODULE_MATERIAL);
        long coursewareCount = countBy(null, MODULE_COURSEWARE);
        long markdownCount = countBy(null, MODULE_MARKDOWN_CONVERSION) + countBy(null, MODULE_MARKDOWN_BATCH);

        LambdaQueryWrapper<LLMCallLog> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.orderByDesc(LLMCallLog::getCreatedAt)
                .orderByDesc(LLMCallLog::getId)
                .last("LIMIT " + RECENT_LOG_LIMIT);

        List<LLMCallLog> recentLogs = llmCallLogMapper.selectList(recentWrapper);
        double successRate = totalCalls == 0 ? 0.0 : Math.round((successCount * 1000.0 / totalCalls)) / 10.0;

        return new LLMStatisticsDTO(
                totalCalls,
                successCount,
                failedCount,
                successRate,
                objectiveCount,
                syllabusCount,
                materialCount,
                coursewareCount,
                markdownCount,
                toDtoList(recentLogs)
        );
    }

    private void saveLog(Integer userId,
                         Integer courseId,
                         String moduleType,
                         String requestSummary,
                         String status,
                         String errorMessage) {
        if (userId == null) {
            return;
        }

        LLMCallLog log = new LLMCallLog();
        log.setUserId(userId);
        log.setCourseId(courseId);
        log.setModuleType(normalizeModuleType(moduleType));
        log.setRequestSummary(truncate(normalizeText(requestSummary), MAX_SUMMARY_LENGTH));
        log.setStatus(status);
        log.setErrorMessage(truncate(normalizeText(errorMessage), MAX_ERROR_LENGTH));
        log.setCreatedAt(new Date());
        llmCallLogMapper.insert(log);
    }

    private long countBy(String status, String moduleType) {
        LambdaQueryWrapper<LLMCallLog> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(LLMCallLog::getStatus, status);
        }
        if (moduleType != null) {
            wrapper.eq(LLMCallLog::getModuleType, moduleType);
        }
        return llmCallLogMapper.selectCount(wrapper);
    }

    private List<LLMCallLogDTO> toDtoList(List<LLMCallLog> logs) {
        List<LLMCallLogDTO> results = new ArrayList<>();
        for (LLMCallLog log : logs) {
            User user = log.getUserId() == null ? null : userMapper.selectById(log.getUserId());
            results.add(new LLMCallLogDTO(
                    log.getId(),
                    log.getUserId(),
                    user == null ? "未知用户" : user.getUsername(),
                    log.getCourseId(),
                    log.getModuleType(),
                    log.getRequestSummary(),
                    log.getStatus(),
                    log.getErrorMessage(),
                    log.getCreatedAt()
            ));
        }
        return results;
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        String message = throwable.getMessage();
        if (message != null && !message.trim().isEmpty()) {
            return message.trim();
        }

        Throwable cause = throwable.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().trim().isEmpty()) {
            return cause.getMessage().trim();
        }

        return throwable.getClass().getSimpleName();
    }

    private String normalizeModuleType(String moduleType) {
        String normalized = normalizeText(moduleType);
        return normalized.isEmpty() ? "unknown" : normalized;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
