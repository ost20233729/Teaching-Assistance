package com.java_web.backend.Common.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_web.backend.Common.Config.OpenAIConfig;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Utils.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LLMJsonToMarkdownService {

    @Autowired
    private OpenAIConfig openAIConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String convertJsonToMarkdown(String jsonContent, String outputFormat, String customStyle) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            throw ApiException.badRequest("jsonContent不能为空");
        }

        if (!isValidJson(jsonContent)) {
            throw ApiException.badRequest("jsonContent不是合法的JSON");
        }

        try {
            Map<String, Object> structureInfo = parseJsonStructure(jsonContent);
            String prompt = buildPrompt(jsonContent, structureInfo, outputFormat, customStyle);
            return callLLM(prompt);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ApiException.badGateway("Markdown 转换失败，请稍后重试");
        }
    }

    public String convertJsonToMarkdownWithCustomStyle(String jsonContent, String customStyle) {
        return convertJsonToMarkdown(jsonContent, "markdown", customStyle);
    }

    public Map<String, String> batchConvertJsonToMarkdown(Map<String, String> jsonContents) {
        Map<String, String> results = new HashMap<>();
        for (Map.Entry<String, String> entry : jsonContents.entrySet()) {
            try {
                String result = convertJsonToMarkdown(entry.getValue(), "markdown", "默认样式");
                results.put(entry.getKey(), result);
            } catch (Exception exception) {
                results.put(entry.getKey(), "转换失败: " + exception.getMessage());
            }
        }
        return results;
    }

    private boolean isValidJson(String jsonContent) {
        try {
            objectMapper.readTree(jsonContent);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private Map<String, Object> parseJsonStructure(String jsonContent) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonContent);
            Map<String, Object> structureInfo = new HashMap<>();

            if (jsonNode.isObject()) {
                structureInfo.put("type", "object");
                List<String> keys = new ArrayList<>();
                jsonNode.fieldNames().forEachRemaining(keys::add);
                structureInfo.put("keys", keys);
                structureInfo.put("length", jsonNode.size());
                structureInfo.put("hasNested", hasNestedStructure(jsonNode, 0, 3));
            } else if (jsonNode.isArray()) {
                structureInfo.put("type", "array");
                structureInfo.put("length", jsonNode.size());
                structureInfo.put("hasNested", hasNestedStructure(jsonNode, 0, 3));
            } else {
                structureInfo.put("type", "primitive");
                structureInfo.put("length", 1);
                structureInfo.put("hasNested", false);
            }

            return structureInfo;
        } catch (Exception exception) {
            throw ApiException.badGateway("JSON结构解析失败，请稍后重试");
        }
    }

    private boolean hasNestedStructure(JsonNode node, int currentDepth, int maxDepth) {
        if (currentDepth >= maxDepth) {
            return false;
        }

        if (node.isObject() || node.isArray()) {
            for (JsonNode child : node) {
                if (child.isObject() || child.isArray() || hasNestedStructure(child, currentDepth + 1, maxDepth)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String buildPrompt(String jsonContent, Map<String, Object> structureInfo, String outputFormat, String customStyle) {
        String promptTemplate = loadPromptFromFile("prompt/json_to_markdown/json_to_markdown_prompt.txt");
        String resolvedOutputFormat = outputFormat == null || outputFormat.trim().isEmpty() ? "markdown" : outputFormat;
        String resolvedCustomStyle = customStyle == null ? "" : customStyle;

        StringBuilder builder = new StringBuilder();
        builder.append(promptTemplate == null ? getDefaultPrompt() : promptTemplate);
        builder.append("\n\nJSON内容：\n").append(jsonContent);
        builder.append("\n\n结构信息：\n").append(structureInfo);
        builder.append("\n\n输出格式：").append(resolvedOutputFormat);
        builder.append("\n自定义样式：").append(resolvedCustomStyle);
        return builder.toString();
    }

    public String callLLM(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openAIConfig.getModelName());

            List<Map<String, Object>> messages = new ArrayList<>();

            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是一个专业的JSON转Markdown助手，输出内容必须直接可用。");
            messages.add(systemMsg);

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 8000);

            String response = HttpUtil.postJsonWithApiKey(
                openAIConfig.getApiUrl(),
                openAIConfig.getApiKey(),
                requestBody
            );

            JsonNode responseNode = objectMapper.readTree(response);
            if (responseNode.has("choices") && responseNode.get("choices").isArray() && responseNode.get("choices").size() > 0) {
                JsonNode choice = responseNode.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    return choice.get("message").get("content").asText();
                }
            }

            throw ApiException.badGateway("大模型响应格式错误");
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ApiException.badGateway("Markdown 转换失败，请稍后重试");
        }
    }

    private String loadPromptFromFile(String filePath) {
        try {
            ClassPathResource resource = new ClassPathResource(filePath);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return null;
        }
    }

    private String getDefaultPrompt() {
        return "请将输入的JSON内容整理为结构清晰、层次明确、可直接阅读的Markdown文档。";
    }

    public Map<String, Object> batchConvert(String[] jsonContents, String outputFormat, String customStyle) {
        Map<String, Object> results = new HashMap<>();
        int success = 0;
        int error = 0;

        for (int i = 0; i < jsonContents.length; i++) {
            try {
                String markdownContent = convertJsonToMarkdown(jsonContents[i], outputFormat, customStyle);
                Map<String, Object> itemResult = new HashMap<>();
                itemResult.put("status", "success");
                itemResult.put("content", markdownContent);
                results.put("item_" + i, itemResult);
                success++;
            } catch (Exception exception) {
                Map<String, Object> itemResult = new HashMap<>();
                itemResult.put("status", "error");
                itemResult.put("error", exception.getMessage());
                results.put("item_" + i, itemResult);
                error++;
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", jsonContents.length);
        summary.put("success", success);
        summary.put("error", error);
        summary.put("successRate", jsonContents.length == 0 ? "0.0%" : String.format("%.1f%%", (double) success / jsonContents.length * 100));
        results.put("summary", summary);

        return results;
    }
}
