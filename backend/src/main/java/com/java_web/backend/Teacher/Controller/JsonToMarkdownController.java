package com.java_web.backend.Teacher.Controller;

import com.java_web.backend.Common.Entity.JsonToMarkdownRequest;
import com.java_web.backend.Common.Entity.JsonToMarkdownResponse;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Service.LLMCallLogService;
import com.java_web.backend.Common.Service.LLMJsonToMarkdownService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/llm")
public class JsonToMarkdownController {
    @Autowired
    private LLMJsonToMarkdownService jsonToMarkdownService;

    @Autowired
    private LLMCallLogService llmCallLogService;

    @PostMapping("/markdown-conversions")
    public ResponseEntity<JsonToMarkdownResponse> convertJsonToMarkdown(@RequestBody JsonToMarkdownRequest request,
                                                                        HttpServletRequest servletRequest) {
        if (request == null) {
            throw ApiException.badRequest("请求体不能为空");
        }

        Integer userId = (Integer) servletRequest.getAttribute("userId");
        String requestSummary = llmCallLogService.summarizeMarkdownRequest(
                request.getOutputFormat(),
                request.getCustomStyle(),
                request.getJsonContent() == null ? 0 : request.getJsonContent().length()
        );

        try {
            String markdownContent = jsonToMarkdownService.convertJsonToMarkdown(
                    request.getJsonContent(),
                    request.getOutputFormat() != null ? request.getOutputFormat() : "markdown",
                    request.getCustomStyle()
            );
            llmCallLogService.recordSuccess(
                    userId,
                    null,
                    LLMCallLogService.MODULE_MARKDOWN_CONVERSION,
                    requestSummary
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new JsonToMarkdownResponse(markdownContent, "success"));
        } catch (Exception exception) {
            llmCallLogService.recordFailure(
                    userId,
                    null,
                    LLMCallLogService.MODULE_MARKDOWN_CONVERSION,
                    requestSummary,
                    exception
            );
            throw exception;
        }
    }

    @PostMapping("/markdown-conversion-batches")
    public ResponseEntity<Map<String, Object>> batchConvertJsonToMarkdown(@RequestBody Map<String, Object> request,
                                                                          HttpServletRequest servletRequest) {
        if (request == null) {
            throw ApiException.badRequest("请求体不能为空");
        }

        Object jsonContentsValue = request.get("jsonContents");
        if (!(jsonContentsValue instanceof List<?> rawList) || rawList.isEmpty()) {
            throw ApiException.badRequest("jsonContents不能为空");
        }

        for (Object item : rawList) {
            if (!(item instanceof String)) {
                throw ApiException.badRequest("jsonContents 必须是字符串数组");
            }
        }

        @SuppressWarnings("unchecked")
        List<String> jsonContents = (List<String>) jsonContentsValue;
        String outputFormat = (String) request.getOrDefault("outputFormat", "markdown");
        String customStyle = (String) request.get("customStyle");
        Integer userId = (Integer) servletRequest.getAttribute("userId");
        String requestSummary = llmCallLogService.summarizeMarkdownBatchRequest(
                outputFormat,
                customStyle,
                jsonContents.size()
        );

        try {
            Map<String, Object> results = jsonToMarkdownService.batchConvert(
                    jsonContents.toArray(new String[0]),
                    outputFormat,
                    customStyle
            );
            llmCallLogService.recordSuccess(
                    userId,
                    null,
                    LLMCallLogService.MODULE_MARKDOWN_BATCH,
                    requestSummary
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(results);
        } catch (Exception exception) {
            llmCallLogService.recordFailure(
                    userId,
                    null,
                    LLMCallLogService.MODULE_MARKDOWN_BATCH,
                    requestSummary,
                    exception
            );
            throw exception;
        }
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "message", "JSON to Markdown service is running");
    }
}
