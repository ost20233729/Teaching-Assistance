package com.java_web.backend.Common.DTO;

import java.util.ArrayList;
import java.util.List;

public class LLMStatisticsDTO {
    private long totalCalls;
    private long successCount;
    private long failedCount;
    private double successRate;
    private long objectiveCount;
    private long syllabusCount;
    private long materialCount;
    private long coursewareCount;
    private long markdownCount;
    private List<LLMCallLogDTO> recentCalls = new ArrayList<>();

    public LLMStatisticsDTO() {
    }

    public LLMStatisticsDTO(long totalCalls,
                            long successCount,
                            long failedCount,
                            double successRate,
                            long objectiveCount,
                            long syllabusCount,
                            long materialCount,
                            long markdownCount,
                            List<LLMCallLogDTO> recentCalls) {
        this(totalCalls, successCount, failedCount, successRate, objectiveCount, syllabusCount, materialCount, 0, markdownCount, recentCalls);
    }

    public LLMStatisticsDTO(long totalCalls,
                            long successCount,
                            long failedCount,
                            double successRate,
                            long objectiveCount,
                            long syllabusCount,
                            long materialCount,
                            long coursewareCount,
                            long markdownCount,
                            List<LLMCallLogDTO> recentCalls) {
        this.totalCalls = totalCalls;
        this.successCount = successCount;
        this.failedCount = failedCount;
        this.successRate = successRate;
        this.objectiveCount = objectiveCount;
        this.syllabusCount = syllabusCount;
        this.materialCount = materialCount;
        this.coursewareCount = coursewareCount;
        this.markdownCount = markdownCount;
        this.recentCalls = recentCalls;
    }

    public long getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(long totalCalls) {
        this.totalCalls = totalCalls;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public long getObjectiveCount() {
        return objectiveCount;
    }

    public void setObjectiveCount(long objectiveCount) {
        this.objectiveCount = objectiveCount;
    }

    public long getSyllabusCount() {
        return syllabusCount;
    }

    public void setSyllabusCount(long syllabusCount) {
        this.syllabusCount = syllabusCount;
    }

    public long getMaterialCount() {
        return materialCount;
    }

    public void setMaterialCount(long materialCount) {
        this.materialCount = materialCount;
    }

    public long getCoursewareCount() {
        return coursewareCount;
    }

    public void setCoursewareCount(long coursewareCount) {
        this.coursewareCount = coursewareCount;
    }

    public long getMarkdownCount() {
        return markdownCount;
    }

    public void setMarkdownCount(long markdownCount) {
        this.markdownCount = markdownCount;
    }

    public List<LLMCallLogDTO> getRecentCalls() {
        return recentCalls;
    }

    public void setRecentCalls(List<LLMCallLogDTO> recentCalls) {
        this.recentCalls = recentCalls;
    }
}
