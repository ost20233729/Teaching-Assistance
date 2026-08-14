package com.java_web.backend.Teacher.Controller;

import com.java_web.backend.Common.DTO.MessageResponse;
import com.java_web.backend.Common.Entity.Notification;
import com.java_web.backend.Common.Service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public List<Notification> getCurrentUserNotifications(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return notificationService.getUserNotifications(userId);
    }

    @PatchMapping("/{notificationId}/read")
    public Notification markAsRead(@PathVariable Long notificationId, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return notificationService.markAsRead(userId, notificationId);
    }

    @PatchMapping("/read-all")
    public MessageResponse markAllAsRead(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return notificationService.markAllAsRead(userId);
    }
}
