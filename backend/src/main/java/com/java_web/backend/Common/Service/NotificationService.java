package com.java_web.backend.Common.Service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.java_web.backend.Common.DTO.MessageResponse;
import com.java_web.backend.Common.Entity.Notification;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class NotificationService {
    public static final String TYPE_COURSE_APPROVED = "course_approved";
    public static final String TYPE_COURSE_REJECTED = "course_rejected";
    public static final String TYPE_RESTRICTION_ADDED = "restriction_added";
    public static final String TYPE_RESTRICTION_REMOVED = "restriction_removed";

    @Autowired
    private NotificationMapper notificationMapper;

    public List<Notification> getUserNotifications(Integer userId) {
        return notificationMapper.selectByUserId(userId);
    }

    public Notification markAsRead(Integer userId, Long notificationId) {
        Notification notification = notificationMapper.selectByUserIdAndId(userId, notificationId);
        if (notification == null) {
            throw ApiException.notFound("通知不存在");
        }

        if (!Integer.valueOf(1).equals(notification.getIsRead())) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }

        return notification;
    }

    public MessageResponse markAllAsRead(Integer userId) {
        LambdaUpdateWrapper<Notification> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1);
        notificationMapper.update(null, updateWrapper);
        return new MessageResponse("已将全部通知标记为已读");
    }

    public void createCourseReviewNotification(Integer userId, String courseName, String status, String reviewComment) {
        if (userId == null || courseName == null || courseName.trim().isEmpty()) {
            return;
        }

        String normalizedCourseName = courseName.trim();
        String normalizedComment = normalizeText(reviewComment);

        if ("approved".equals(status)) {
            String content = "课程《" + normalizedCourseName + "》已审核通过，可以继续进入课程介绍、课程大纲、教学讲义和教学课件提纲模块。";
            if (normalizedComment != null) {
                content += " 审核意见：" + normalizedComment;
            }
            createNotification(userId, "课程审核已通过", content, TYPE_COURSE_APPROVED);
            return;
        }

        if ("rejected".equals(status)) {
            String content = "课程《" + normalizedCourseName + "》未通过审核，请根据审核意见修改后再提交。";
            if (normalizedComment != null) {
                content += " 审核意见：" + normalizedComment;
            }
            createNotification(userId, "课程审核未通过", content, TYPE_COURSE_REJECTED);
        }
    }

    public void createRestrictionNotification(Integer userId, String functionName, boolean restricted) {
        if (userId == null || functionName == null || functionName.trim().isEmpty()) {
            return;
        }

        String functionLabel = toFunctionLabel(functionName.trim());
        if (restricted) {
            createNotification(
                    userId,
                    "功能限制已更新",
                    "管理员已限制你使用“" + functionLabel + "”模块，请在限制解除后再继续相关操作。",
                    TYPE_RESTRICTION_ADDED
            );
            return;
        }

        createNotification(
                userId,
                "功能限制已解除",
                "管理员已解除你对“" + functionLabel + "”模块的限制，你现在可以继续使用该功能。",
                TYPE_RESTRICTION_REMOVED
        );
    }

    public Notification createNotification(Integer userId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);
        notification.setCreatedAt(new Date());
        notificationMapper.insert(notification);
        return notification;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private String toFunctionLabel(String functionName) {
        return switch (functionName) {
            case "basic" -> "课程介绍与教学目标";
            case "outline" -> "课程大纲";
            case "lecture" -> "教学讲义";
            case "courseware" -> "教学课件提纲";
            default -> functionName;
        };
    }
}
