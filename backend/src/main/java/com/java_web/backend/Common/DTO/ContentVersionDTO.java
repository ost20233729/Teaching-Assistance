package com.java_web.backend.Common.DTO;

import java.util.Date;

public class ContentVersionDTO {
    private Long id;
    private Integer courseId;
    private String moduleType;
    private String preview;
    private String content;
    private Integer createdBy;
    private Date createdAt;

    public ContentVersionDTO() {
    }

    public ContentVersionDTO(Long id,
                             Integer courseId,
                             String moduleType,
                             String preview,
                             String content,
                             Integer createdBy,
                             Date createdAt) {
        this.id = id;
        this.courseId = courseId;
        this.moduleType = moduleType;
        this.preview = preview;
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

    public String getPreview() {
        return preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
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
