package com.java_web.backend.service;

import com.java_web.backend.Common.DTO.PromptTemplateDTO;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Teacher.Service.PromptTemplateService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptTemplateServiceTest {
    private final PromptTemplateService promptTemplateService = new PromptTemplateService();

    @Test
    void getPromptTemplates_ShouldReturnAllTemplatesWhenModuleIsEmpty() {
        List<PromptTemplateDTO> templates = promptTemplateService.getPromptTemplates(null);

        assertEquals(12, templates.size());
        assertTrue(templates.stream().anyMatch(template -> "objective".equals(template.getModule())));
        assertTrue(templates.stream().anyMatch(template -> "syllabus".equals(template.getModule())));
        assertTrue(templates.stream().anyMatch(template -> "material".equals(template.getModule())));
        assertTrue(templates.stream().anyMatch(template -> "courseware".equals(template.getModule())));
    }

    @Test
    void getPromptTemplates_ShouldReturnFilteredTemplates() {
        List<PromptTemplateDTO> templates = promptTemplateService.getPromptTemplates("syllabus");

        assertEquals(3, templates.size());
        assertTrue(templates.stream().allMatch(template -> "syllabus".equals(template.getModule())));
    }

    @Test
    void getPromptTemplates_ShouldRejectUnsupportedModule() {
        ApiException exception = assertThrows(ApiException.class, () -> promptTemplateService.getPromptTemplates("unknown"));

        assertEquals(400, exception.getStatus().value());
        assertEquals("不支持的模板模块: unknown", exception.getMessage());
    }
}
