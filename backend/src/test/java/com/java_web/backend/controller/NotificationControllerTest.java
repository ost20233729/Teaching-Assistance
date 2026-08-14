package com.java_web.backend.controller;

import com.java_web.backend.Common.Entity.Notification;
import com.java_web.backend.Common.Service.JWTService;
import com.java_web.backend.Common.Service.NotificationService;
import com.java_web.backend.Teacher.Controller.NotificationController;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        when(claims.get("role", String.class)).thenReturn("teacher");
        when(claims.get("id")).thenReturn(2);
        when(claims.getSubject()).thenReturn("teacher");
        when(jwtService.parseToken(anyString())).thenReturn(claims);
    }

    @Test
    void getCurrentUserNotifications_ShouldReturnNotificationList() throws Exception {
        Notification notification = new Notification();
        notification.setId(11L);
        notification.setUserId(2);
        notification.setTitle("课程审核已通过");
        notification.setContent("课程《软件工程基础》已审核通过。");
        notification.setType("course_approved");
        notification.setIsRead(0);

        when(notificationService.getUserNotifications(2)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/v1/teacher/notifications")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("课程审核已通过"))
                .andExpect(jsonPath("$[0].isRead").value(0));
    }

    @Test
    void markAsRead_ShouldReturnUpdatedNotification() throws Exception {
        Notification notification = new Notification();
        notification.setId(11L);
        notification.setUserId(2);
        notification.setTitle("课程审核已通过");
        notification.setType("course_approved");
        notification.setIsRead(1);

        when(notificationService.markAsRead(2, 11L)).thenReturn(notification);

        mockMvc.perform(patch("/api/v1/teacher/notifications/11/read")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.isRead").value(1));
    }
}
