package com.java_web.backend.Common.Entity;

import com.baomidou.mybatisplus.annotation.TableId;

import java.util.Date;

public class Courseware {
    @TableId
    private Integer courseId;
    private String content;
    private Date createdAt;
    private Date updatedAt;

    public Courseware() {
    }

    public Courseware(Integer courseId, String content, Date createdAt, Date updatedAt) {
        this.courseId = courseId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
