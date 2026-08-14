package com.java_web.backend.service;

import com.java_web.backend.Admin.Service.AdminRestrictionService;
import com.java_web.backend.Common.Entity.Restriction;
import com.java_web.backend.Common.Entity.User;
import com.java_web.backend.Common.Mapper.RestrictionMapper;
import com.java_web.backend.Common.Mapper.UserMapper;
import com.java_web.backend.Common.Service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRestrictionServiceTest {

    @Mock
    private RestrictionMapper restrictionMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminRestrictionService adminRestrictionService;

    @Test
    void addRestriction_ShouldCreateNotification() {
        User user = new User();
        user.setId(2);
        user.setRole("teacher");
        user.setIsDeleted(0);

        Restriction restriction = new Restriction();
        restriction.setFunctionName("outline");

        when(userMapper.selectById(2)).thenReturn(user);
        when(restrictionMapper.selectByUserIdAndFunction(2, "outline")).thenReturn(null);

        Restriction saved = adminRestrictionService.addRestriction(2, restriction);

        assertEquals(2, saved.getUserId());
        assertEquals("outline", saved.getFunctionName());
        verify(restrictionMapper).insert(any(Restriction.class));
        verify(notificationService).createRestrictionNotification(2, "outline", true);
    }

    @Test
    void removeRestriction_ShouldCreateNotification() {
        Restriction restriction = new Restriction();
        restriction.setId(5);
        restriction.setUserId(2);
        restriction.setFunctionName("lecture");

        when(restrictionMapper.selectById(5)).thenReturn(restriction);

        adminRestrictionService.removeRestriction(2, 5);

        verify(notificationService).createRestrictionNotification(2, "lecture", false);
        verify(restrictionMapper).deleteById(5);
    }
}
