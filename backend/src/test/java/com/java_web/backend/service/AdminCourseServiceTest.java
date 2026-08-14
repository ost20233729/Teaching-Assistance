package com.java_web.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.java_web.backend.Admin.Service.AdminCourseService;
import com.java_web.backend.Common.DTO.PagedResponse;
import com.java_web.backend.Common.Entity.Course;
import com.java_web.backend.Common.Entity.User;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.CourseMapper;
import com.java_web.backend.Common.Mapper.UserMapper;
import com.java_web.backend.Common.Service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCourseServiceTest {

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminCourseService adminCourseService;

    @Test
    void getCourses_ShouldReturnPagedResponse() {
        User teacher = new User();
        teacher.setId(2);
        teacher.setUsername("teacherA");
        teacher.setEmail("teacherA@example.com");

        Course course = new Course();
        course.setId(8);
        course.setTeacherId(2);
        course.setName("人工智能基础");
        course.setStatus("pending");
        course.setIsDeleted(0);

        Page<Course> pageData = new Page<>(1, 8);
        pageData.setRecords(List.of(course));
        pageData.setTotal(5);

        when(userMapper.selectList(any())).thenReturn(List.of(teacher));
        when(courseMapper.selectPage(any(Page.class), any())).thenReturn(pageData);

        PagedResponse<Course> response = adminCourseService.getCourses("pending", "teacherA", 1, 8);

        assertEquals(1, response.getItems().size());
        assertEquals("人工智能基础", response.getItems().get(0).getName());
        assertEquals(5, response.getTotal());
        assertEquals(1, response.getPage());
        assertEquals(8, response.getPageSize());
        verify(courseMapper).selectPage(argThat(page -> page.getCurrent() == 1 && page.getSize() == 8), any());
    }

    @Test
    void updateCourseStatus_ShouldPersistReviewCommentAndReviewedAt() {
        Course course = new Course();
        course.setId(8);
        course.setTeacherId(2);
        course.setName("人工智能基础");
        course.setStatus("pending");
        course.setIsDeleted(0);

        when(courseMapper.selectById(8)).thenReturn(course);

        Course updatedCourse = adminCourseService.updateCourseStatus(8, "rejected", "请补充课程目标与审核说明");

        assertEquals("rejected", updatedCourse.getStatus());
        assertEquals("请补充课程目标与审核说明", updatedCourse.getReviewComment());
        assertNotNull(updatedCourse.getReviewedAt());
        verify(courseMapper).update(isNull(), any());
        verify(notificationService).createCourseReviewNotification(2, "人工智能基础", "rejected", "请补充课程目标与审核说明");
    }

    @Test
    void updateCourseStatus_ShouldRequireCommentForRejectedCourse() {
        ApiException exception = assertThrows(ApiException.class,
                () -> adminCourseService.updateCourseStatus(8, "rejected", "   "));

        assertEquals(400, exception.getStatus().value());
        assertEquals("驳回课程时必须填写审核意见", exception.getMessage());
    }

    @Test
    void updateCourseStatus_ShouldClearReviewFieldsWhenResetToPending() {
        Course course = new Course();
        course.setId(9);
        course.setTeacherId(3);
        course.setName("软件工程");
        course.setStatus("rejected");
        course.setReviewComment("请补充考核方式说明");
        course.setReviewedAt(new Date());
        course.setIsDeleted(0);

        when(courseMapper.selectById(9)).thenReturn(course);

        Course updatedCourse = adminCourseService.updateCourseStatus(9, "pending", "");

        assertEquals("pending", updatedCourse.getStatus());
        assertNull(updatedCourse.getReviewComment());
        assertNull(updatedCourse.getReviewedAt());
        verify(courseMapper).update(isNull(), any());
    }
}
