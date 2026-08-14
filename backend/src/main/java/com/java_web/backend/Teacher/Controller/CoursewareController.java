package com.java_web.backend.Teacher.Controller;

import com.java_web.backend.Common.DTO.PromptRequestDTO;
import com.java_web.backend.Common.Entity.Courseware;
import com.java_web.backend.Teacher.Service.CoursewareService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher/courses")
public class CoursewareController {
    @Autowired
    private CoursewareService coursewareService;

    @GetMapping("/{courseId}/courseware")
    public Courseware getCourseware(@PathVariable Integer courseId, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return coursewareService.getCourseware(courseId, teacherId);
    }

    @PostMapping("/{courseId}/courseware-generations")
    public ResponseEntity<String> generateCourseware(@PathVariable Integer courseId,
                                                     @RequestBody PromptRequestDTO promptRequestDTO,
                                                     HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        String content = coursewareService.generateCoursewareContent(courseId, promptRequestDTO.getPrompt(), teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(content);
    }

    @PutMapping("/{courseId}/courseware")
    public Courseware saveCourseware(@PathVariable Integer courseId,
                                     @RequestBody Courseware courseware,
                                     HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        courseware.setCourseId(courseId);
        return coursewareService.saveCourseware(courseware, teacherId);
    }
}
