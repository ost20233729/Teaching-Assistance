package com.java_web.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_web.backend.Common.Entity.JsonToMarkdownRequest;
import com.java_web.backend.Common.Service.JWTService;
import com.java_web.backend.Common.Service.LLMCallLogService;
import com.java_web.backend.Common.Service.LLMJsonToMarkdownService;
import com.java_web.backend.Teacher.Controller.JsonToMarkdownController;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JsonToMarkdownController.class)
@Import(JsonToMarkdownController.class)
class JsonToMarkdownControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LLMJsonToMarkdownService jsonToMarkdownService;

    @MockBean
    private LLMCallLogService llmCallLogService;

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
    void convertJsonToMarkdown_Success() throws Exception {
        JsonToMarkdownRequest request = new JsonToMarkdownRequest();
        request.setJsonContent("{\"name\":\"Alice\"}");
        request.setOutputFormat("markdown");

        when(jsonToMarkdownService.convertJsonToMarkdown(anyString(), anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("# Title");

        mockMvc.perform(post("/api/v1/llm/markdown-conversions")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.markdownContent").value("# Title"));
    }

    @Test
    void batchConvertJsonToMarkdown_Success() throws Exception {
        Map<String, Object> request = Map.of(
                "jsonContents", List.of("{\"name\":\"Alice\"}", "{\"name\":\"Bob\"}"),
                "outputFormat", "markdown"
        );

        when(jsonToMarkdownService.batchConvert(org.mockito.ArgumentMatchers.any(String[].class), anyString(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(Map.of("summary", Map.of("total", 2)));

        mockMvc.perform(post("/api/v1/llm/markdown-conversion-batches")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.summary.total").value(2));
    }

    @Test
    void health_Success() throws Exception {
        mockMvc.perform(get("/api/v1/llm/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
