package com.java_web.backend.Common.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_web.backend.Common.Config.OpenAIConfig;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Utils.HttpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LLMCoursewareService {
    private static final String SYSTEM_PROMPT = "你是一名高校课程设计助理，请基于课程资料生成可直接用于课堂展示的课件提纲 Markdown。"
            + " 输出应结构清晰、标题明确、适合教师继续加工，不要输出代码块围栏。";

    @Autowired
    private OpenAIConfig openAIConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateCourseware(String courseName,
                                     String courseContent,
                                     String teachingTarget,
                                     String syllabusContent,
                                     String customPrompt) {
        String prompt = buildPrompt(courseName, courseContent, teachingTarget, syllabusContent, customPrompt);

        try {
            return callLlm(prompt);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ApiException.badGateway("教学课件提纲生成失败，请稍后重试");
        }
    }

    private String buildPrompt(String courseName,
                               String courseContent,
                               String teachingTarget,
                               String syllabusContent,
                               String customPrompt) {
        StringBuilder builder = new StringBuilder();
        builder.append("请根据以下课程资料生成“教学课件提纲”Markdown。")
                .append("\n\n输出要求：")
                .append("\n1. 以 Markdown 输出，使用多级标题组织结构。")
                .append("\n2. 至少包含：课程定位页、教学目标页、章节安排页、重点难点页、案例/活动建议页、总结与作业页。")
                .append("\n3. 每一页或每一部分都要给出建议标题和要点列表。")
                .append("\n4. 内容面向教师备课，不要生成完整 PPT，只输出课件提纲。")
                .append("\n5. 如果用户给了额外要求，请优先满足。")
                .append("\n\n课程名称：").append(defaultText(courseName))
                .append("\n\n课程介绍：\n").append(defaultText(courseContent))
                .append("\n\n教学目标：\n").append(defaultText(teachingTarget))
                .append("\n\n课程大纲：\n").append(defaultText(syllabusContent))
                .append("\n\n补充要求：\n").append(defaultText(customPrompt));
        return builder.toString();
    }

    private String callLlm(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", openAIConfig.getModelName());

            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", SYSTEM_PROMPT);
            messages.add(systemMessage);

            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.4);
            requestBody.put("max_tokens", 4000);

            String response = HttpUtil.postJsonWithApiKey(
                    openAIConfig.getApiUrl(),
                    openAIConfig.getApiKey(),
                    requestBody
            );

            JsonNode responseNode = objectMapper.readTree(response);
            JsonNode choices = responseNode.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw ApiException.badGateway("教学课件提纲生成失败，请稍后重试");
            }

            JsonNode contentNode = choices.get(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().trim().isEmpty()) {
                throw ApiException.badGateway("教学课件提纲生成失败，请稍后重试");
            }

            return contentNode.asText().trim();
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ApiException.badGateway("教学课件提纲生成失败，请稍后重试");
        }
    }

    private String defaultText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "无";
        }
        return value.trim();
    }
}
