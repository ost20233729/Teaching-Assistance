package com.java_web.backend.Common.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName("llm_call_log")
public class LLMCallLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private Integer courseId;
    private String moduleType;
    private String requestSummary;
    private String status;
    private String errorMessage;
    private Date createdAt;

    public LLMCallLog() {
    }

    public LLMCallLog(Long id,
                      Integer userId,
                      Integer courseId,
                      String moduleType,
                      String requestSummary,
                      String status,
                      String errorMessage,
                      Date createdAt) {
        this.id = id;
        this.userId = userId;
        this.courseId = courseId;
        this.moduleType = moduleType;
        this.requestSummary = requestSummary;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public String getRequestSummary() {
        return requestSummary;
    }

    public void setRequestSummary(String requestSummary) {
        this.requestSummary = requestSummary;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
