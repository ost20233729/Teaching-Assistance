package com.java_web.backend.controller;

import com.java_web.backend.Common.DTO.PromptTemplateDTO;
import com.java_web.backend.Common.Service.JWTService;
import com.java_web.backend.Teacher.Controller.PromptTemplateController;
import com.java_web.backend.Teacher.Service.PromptTemplateService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PromptTemplateController.class)
@Import(PromptTemplateController.class)
class PromptTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PromptTemplateService promptTemplateService;

    @MockBean
    private JWTService jwtService;

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        when(claims.get("role", String.class)).thenReturn("teacher");
        when(claims.get("id")).thenReturn(1);
        when(claims.getSubject()).thenReturn("tester");
        when(jwtService.parseToken(anyString())).thenReturn(claims);
    }

    @Test
    void getPromptTemplates_ByModule() throws Exception {
        when(promptTemplateService.getPromptTemplates("objective")).thenReturn(List.of(
                new PromptTemplateDTO(
                        "objective-general",
                        "objective",
                        "课程介绍模板",
                        "专业基础课通用模板",
                        "适合本科专业基础课，突出课程定位、先修基础和学习价值。",
                        "示例提示词"
                )
        ));

        mockMvc.perform(get("/api/v1/teacher/prompt-templates")
                        .header("Authorization", "Bearer test-token")
                        .param("module", "objective"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("objective-general"))
                .andExpect(jsonPath("$[0].module").value("objective"))
                .andExpect(jsonPath("$[0].name").value("专业基础课通用模板"));
    }
}
