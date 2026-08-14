package com.java_web.backend.service;

import com.java_web.backend.Common.Entity.Notification;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.NotificationMapper;
import com.java_web.backend.Common.Service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void markAsRead_ShouldUpdateUnreadNotification() {
        Notification notification = new Notification();
        notification.setId(3L);
        notification.setUserId(2);
        notification.setTitle("课程审核已通过");
        notification.setIsRead(0);

        when(notificationMapper.selectByUserIdAndId(2, 3L)).thenReturn(notification);

        Notification result = notificationService.markAsRead(2, 3L);

        assertEquals(1, result.getIsRead());
        verify(notificationMapper).updateById(notification);
    }

    @Test
    void markAsRead_ShouldThrowWhenNotificationMissing() {
        when(notificationMapper.selectByUserIdAndId(2, 8L)).thenReturn(null);

        ApiException exception = assertThrows(ApiException.class, () -> notificationService.markAsRead(2, 8L));

        assertEquals(404, exception.getStatus().value());
        assertEquals("通知不存在", exception.getMessage());
    }

    @Test
    void createCourseReviewNotification_ShouldInsertApprovedNotification() {
        notificationService.createCourseReviewNotification(2, "软件工程基础", "approved", "教学目标完整");

        verify(notificationMapper).insert(argThat(matchesNotification(
                2,
                "课程审核已通过",
                NotificationService.TYPE_COURSE_APPROVED,
                "软件工程基础",
                "教学目标完整"
        )));
    }

    @Test
    void createRestrictionNotification_ShouldInsertRemovalNotification() {
        notificationService.createRestrictionNotification(2, "courseware", false);

        verify(notificationMapper).insert(argThat(matchesNotification(
                2,
                "功能限制已解除",
                NotificationService.TYPE_RESTRICTION_REMOVED,
                "教学课件提纲",
                "解除"
        )));
    }

    private ArgumentMatcher<Notification> matchesNotification(Integer userId,
                                                              String title,
                                                              String type,
                                                              String contentKeywordOne,
                                                              String contentKeywordTwo) {
        return notification -> notification != null
                && userId.equals(notification.getUserId())
                && title.equals(notification.getTitle())
                && type.equals(notification.getType())
                && Integer.valueOf(0).equals(notification.getIsRead())
                && notification.getContent() != null
                && notification.getContent().contains(contentKeywordOne)
                && notification.getContent().contains(contentKeywordTwo);
    }
}
