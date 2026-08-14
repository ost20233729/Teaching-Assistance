package com.java_web.backend.Teacher.Controller;

import com.java_web.backend.Common.DTO.CourseExportDTO;
import com.java_web.backend.Common.DTO.CoursePayloadDTO;
import com.java_web.backend.Common.DTO.PagedResponse;
import com.java_web.backend.Common.Entity.Course;
import com.java_web.backend.Teacher.Service.CourseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher/courses")
public class CourseController {
    @Autowired
    private CourseService courseService;

    @GetMapping
    public PagedResponse<Course> getCoursesByTeacher(HttpServletRequest request,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer pageSize) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return courseService.getTeacherCourses(teacherId, keyword, status, page, pageSize);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getCourseById(@PathVariable int id, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return courseService.getCourseDetail(id, teacherId);
    }

    @GetMapping("/{id}/export/markdown")
    public ResponseEntity<byte[]> exportCourseMarkdown(@PathVariable int id, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        CourseExportDTO export = courseService.exportCourseMarkdown(id, teacherId);
        byte[] content = export.getContent().getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.getFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .contentLength(content.length)
                .body(content);
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody CoursePayloadDTO payload, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        Course course = courseService.createCourse(payload.getCourseName(), teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(course);
    }

    @PatchMapping("/{id}")
    public Course updateCourseName(@PathVariable int id,
                                   @RequestBody CoursePayloadDTO payload,
                                   HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return courseService.updateCourseName(id, payload.getCourseName(), teacherId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable int id, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        courseService.deleteCourse(id, teacherId);
        return ResponseEntity.noContent().build();
    }
}
