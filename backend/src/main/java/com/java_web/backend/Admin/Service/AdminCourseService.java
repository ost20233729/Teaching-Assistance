package com.java_web.backend.Admin.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.java_web.backend.Common.DTO.PagedResponse;
import com.java_web.backend.Common.Entity.Course;
import com.java_web.backend.Common.Entity.User;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.CourseMapper;
import com.java_web.backend.Common.Mapper.UserMapper;
import com.java_web.backend.Common.Service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminCourseService {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 8;
    private static final int MAX_PAGE_SIZE = 50;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    public PagedResponse<Course> getCourses(String status, String keyword, Integer page, Integer pageSize) {
        String normalizedStatus = normalizeStatusFilter(status);
        String normalizedKeyword = normalizeKeyword(keyword);
        long resolvedPage = resolvePage(page);
        long resolvedPageSize = resolvePageSize(pageSize);

        LambdaQueryWrapper<Course> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Course::getIsDeleted, 0);

        if (normalizedStatus != null) {
            queryWrapper.eq(Course::getStatus, normalizedStatus);
        }

        if (normalizedKeyword != null) {
            List<Integer> matchedTeacherIds = findTeacherIdsByKeyword(normalizedKeyword);
            Integer matchedCourseId = parseCourseIdKeyword(normalizedKeyword);

            queryWrapper.and(wrapper -> {
                wrapper.like(Course::getName, normalizedKeyword)
                        .or()
                        .like(Course::getReviewComment, normalizedKeyword);

                if (matchedCourseId != null) {
                    wrapper.or().eq(Course::getId, matchedCourseId);
                }

                if (!matchedTeacherIds.isEmpty()) {
                    wrapper.or().in(Course::getTeacherId, matchedTeacherIds);
                }
            });
        }

        queryWrapper.orderByDesc(Course::getUpdatedAt)
                .orderByDesc(Course::getCreatedAt)
                .orderByDesc(Course::getId);

        Page<Course> resultPage = courseMapper.selectPage(new Page<>(resolvedPage, resolvedPageSize), queryWrapper);
        return PagedResponse.of(resultPage);
    }

    public Course updateCourseStatus(Integer id, String status, String reviewComment) {
        if (!"approved".equals(status) && !"rejected".equals(status) && !"pending".equals(status)) {
            throw ApiException.badRequest("非法的课程状态");
        }

        String normalizedComment = normalizeReviewComment(reviewComment);
        if ("rejected".equals(status) && (normalizedComment == null || normalizedComment.isEmpty())) {
            throw ApiException.badRequest("驳回课程时必须填写审核意见");
        }

        Course course = courseMapper.selectById(id);
        if (course == null || Integer.valueOf(1).equals(course.getIsDeleted())) {
            throw ApiException.notFound("课程不存在");
        }

        Date operationTime = new Date();
        LambdaUpdateWrapper<Course> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Course::getId, id)
                .set(Course::getStatus, status)
                .set(Course::getReviewComment, "pending".equals(status) ? null : normalizedComment)
                .set(Course::getReviewedAt, "pending".equals(status) ? null : operationTime)
                .set(Course::getUpdatedAt, operationTime);
        courseMapper.update(null, updateWrapper);

        course.setStatus(status);
        if ("pending".equals(status)) {
            course.setReviewComment(null);
            course.setReviewedAt(null);
        } else {
            course.setReviewComment(normalizedComment);
            course.setReviewedAt(operationTime);
            notificationService.createCourseReviewNotification(course.getTeacherId(), course.getName(), status, normalizedComment);
        }
        course.setUpdatedAt(operationTime);
        return course;
    }

    private String normalizeReviewComment(String reviewComment) {
        if (reviewComment == null) {
            return null;
        }

        String trimmedComment = reviewComment.trim();
        if (trimmedComment.isEmpty()) {
            return null;
        }

        if (trimmedComment.length() > 500) {
            throw ApiException.badRequest("审核意见不能超过500个字符");
        }

        return trimmedComment;
    }

    private String normalizeStatusFilter(String status) {
        if (status == null) {
            return null;
        }

        String trimmedStatus = status.trim();
        if (trimmedStatus.isEmpty() || "all".equals(trimmedStatus)) {
            return null;
        }

        if (!"approved".equals(trimmedStatus) && !"rejected".equals(trimmedStatus) && !"pending".equals(trimmedStatus)) {
            throw ApiException.badRequest("非法的课程状态");
        }

        return trimmedStatus;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String trimmedKeyword = keyword.trim();
        return trimmedKeyword.isEmpty() ? null : trimmedKeyword;
    }

    private long resolvePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }

        if (page < 1) {
            throw ApiException.badRequest("页码必须大于 0");
        }

        return page;
    }

    private long resolvePageSize(Integer pageSize) {
        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }

        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("每页条数必须在 1 到 50 之间");
        }

        return pageSize;
    }

    private List<Integer> findTeacherIdsByKeyword(String keyword) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getRole, "teacher")
                .eq(User::getIsDeleted, 0)
                .and(wrapper -> wrapper.like(User::getUsername, keyword)
                        .or()
                        .like(User::getEmail, keyword));

        return userMapper.selectList(queryWrapper)
                .stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    private Integer parseCourseIdKeyword(String keyword) {
        if (!keyword.chars().allMatch(Character::isDigit)) {
            return null;
        }

        try {
            return Integer.parseInt(keyword);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
