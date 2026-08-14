package com.java_web.backend.Teacher.Controller;

import com.java_web.backend.Common.DTO.PromptRequestDTO;
import com.java_web.backend.Common.Entity.Syllabus;
import com.java_web.backend.Teacher.Service.SyllabusService;
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
public class SyllabusController {
    @Autowired
    private SyllabusService syllabusService;

    @GetMapping("/{courseId}/syllabus")
    public Syllabus getSyllabus(@PathVariable int courseId, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return syllabusService.getCourseSyllabus(courseId, teacherId);
    }

    @PostMapping("/{courseId}/syllabus-generations")
    public ResponseEntity<String> generateSyllabus(@PathVariable Integer courseId,
                                                   @RequestBody PromptRequestDTO prompt,
                                                   HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        String syllabus = syllabusService.generateSyllabusContent(courseId, prompt.getPrompt(), teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(syllabus);
    }

    @PutMapping("/{courseId}/syllabus")
    public Syllabus saveSyllabus(@PathVariable Integer courseId,
                                 @RequestBody Syllabus syllabus,
                                 HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        syllabus.setCourseId(courseId);
        return syllabusService.saveSyllabus(syllabus, teacherId);
    }
}
