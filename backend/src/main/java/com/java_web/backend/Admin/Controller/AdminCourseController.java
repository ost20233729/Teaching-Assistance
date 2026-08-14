package com.java_web.backend.Admin.Controller;

import com.java_web.backend.Admin.Service.AdminCourseService;
import com.java_web.backend.Common.DTO.CourseStatusUpdateDTO;
import com.java_web.backend.Common.DTO.PagedResponse;
import com.java_web.backend.Common.Entity.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/courses")
public class AdminCourseController {
    @Autowired
    private AdminCourseService adminCourseService;

    @GetMapping
    public PagedResponse<Course> getCourses(@RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer pageSize) {
        return adminCourseService.getCourses(status, keyword, page, pageSize);
    }

    @PatchMapping("/{id}")
    public Course updateCourseStatus(@PathVariable Integer id, @RequestBody CourseStatusUpdateDTO request) {
        return adminCourseService.updateCourseStatus(id, request.getStatus(), request.getReviewComment());
    }
}
