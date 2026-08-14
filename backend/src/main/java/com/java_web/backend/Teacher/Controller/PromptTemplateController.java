package com.java_web.backend.Teacher.Controller;

import com.java_web.backend.Common.DTO.PromptTemplateDTO;
import com.java_web.backend.Teacher.Service.PromptTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher/prompt-templates")
public class PromptTemplateController {
    @Autowired
    private PromptTemplateService promptTemplateService;

    @GetMapping
    public List<PromptTemplateDTO> getPromptTemplates(@RequestParam(required = false) String module) {
        return promptTemplateService.getPromptTemplates(module);
    }
}
