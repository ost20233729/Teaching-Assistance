package com.java_web.backend.Teacher.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.java_web.backend.Common.DTO.MaterialGenerationRequestDTO;
import com.java_web.backend.Common.Entity.InitialSyllabusRequest;
import com.java_web.backend.Common.Entity.Material;
import com.java_web.backend.Teacher.Service.MaterialService;
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
public class MaterialController {
    @Autowired
    private MaterialService materialService;

    @GetMapping("/{courseId}/material")
    public Material getMaterial(@PathVariable Integer courseId, HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return materialService.getCourseMaterial(courseId, teacherId);
    }

    @PostMapping("/{courseId}/material-generations")
    public ResponseEntity<String> generateMaterial(@PathVariable Integer courseId,
                                                   @RequestBody MaterialGenerationRequestDTO requestBody,
                                                   HttpServletRequest request) throws JsonProcessingException {
        Integer teacherId = (Integer) request.getAttribute("userId");

        InitialSyllabusRequest req = new InitialSyllabusRequest();
        req.setCourseId(courseId.toString());
        req.setCourseTitle(requestBody.getCourseTitle());
        req.setRequest(requestBody.getRequest());

        String result = materialService.generateMaterialContent(req, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{courseId}/material")
    public Material saveMaterial(@PathVariable Integer courseId,
                                 @RequestBody Material material,
                                 HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        material.setCourseId(courseId);
        return materialService.saveMaterial(material, teacherId);
    }
}
