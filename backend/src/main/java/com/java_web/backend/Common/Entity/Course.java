package com.java_web.backend.Common.Entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.util.Date;

public class Course {
    @TableId(type = IdType.AUTO)
    private Integer id;            // 课程ID
    private Integer teacherId;     // 教师ID
    private String name;           // 课程名称
    private String status;         // 审核状态
    private String reviewComment;  // 审核意见
    private Date reviewedAt;       // 审核时间
    private Integer isDeleted;     // 删除标记
    private Date createdAt;        // 创建时间
    private Date updatedAt;        // 更新时间

    public Course() {}

    public Course(Integer id,
                  Integer teacherId,
                  String name,
                  String status,
                  String reviewComment,
                  Date reviewedAt,
                  Integer isDeleted,
                  Date createdAt,
                  Date updatedAt) {
        this.id = id;
        this.teacherId = teacherId;
        this.name = name;
        this.status = status;
        this.reviewComment = reviewComment;
        this.reviewedAt = reviewedAt;
        this.isDeleted = isDeleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
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

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", teacherId=" + teacherId +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", reviewComment='" + reviewComment + '\'' +
                ", reviewedAt=" + reviewedAt +
                ", isDeleted=" + isDeleted +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
