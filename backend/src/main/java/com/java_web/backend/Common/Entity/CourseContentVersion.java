package com.java_web.backend.Common.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.util.Date;

public class CourseContentVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer courseId;
    private String moduleType;
    private String content;
    private Integer createdBy;
    private Date createdAt;

    public CourseContentVersion() {
    }

    public CourseContentVersion(Long id,
                                Integer courseId,
                                String moduleType,
                                String content,
                                Integer createdBy,
                                Date createdAt) {
        this.id = id;
        this.courseId = courseId;
        this.moduleType = moduleType;
        this.content = content;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
