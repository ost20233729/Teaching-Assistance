package com.java_web.backend.Teacher.Controller;

import com.java_web.backend.Common.DTO.PromptRequestDTO;
import com.java_web.backend.Common.Entity.CourseObjective;
import com.java_web.backend.Teacher.Service.CourseObjectiveService;
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

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teacher/courses")
public class CourseObjectiveController {
    @Autowired
    private CourseObjectiveService objectiveService;

    @GetMapping("/{courseId}/objective")
    public CourseObjective getObjective(@PathVariable int courseId, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return objectiveService.getCourseObjective(courseId, teacherId);
    }

    @PostMapping("/{courseId}/objective-generations")
    public ResponseEntity<Map<String, String>> generateObjective(@PathVariable Integer courseId,
                                                                 @RequestBody PromptRequestDTO prompt,
                                                                 HttpServletRequest request) throws IOException {
        Integer teacherId = (Integer) request.getAttribute("userId");
        Map<String, String> result = objectiveService.generateObjectiveContent(courseId, prompt.getPrompt(), teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{courseId}/objective")
    public CourseObjective saveObjective(@PathVariable Integer courseId,
                                         @RequestBody CourseObjective objective,
                                         HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        objective.setCourseId(courseId);
        return objectiveService.saveObjective(objective, teacherId);
    }
}
