package com.java_web.backend.Teacher.Controller;

import com.java_web.backend.Common.DTO.ContentVersionDTO;
import com.java_web.backend.Common.DTO.ContentVersionRestoreResponseDTO;
import com.java_web.backend.Teacher.Service.ContentVersionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/courses")
public class ContentVersionController {
    @Autowired
    private ContentVersionService contentVersionService;

    @GetMapping("/{courseId}/content-versions")
    public List<ContentVersionDTO> getVersions(@PathVariable Integer courseId,
                                               @RequestParam String module,
                                               HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return contentVersionService.listVersions(courseId, module, teacherId);
    }

    @PostMapping("/{courseId}/content-versions/{versionId}/restorations")
    public ContentVersionRestoreResponseDTO restoreVersion(@PathVariable Integer courseId,
                                                           @PathVariable Long versionId,
                                                           HttpServletRequest request) {
        Integer teacherId = (Integer) request.getAttribute("userId");
        return contentVersionService.restoreVersion(courseId, versionId, teacherId);
    }
}
