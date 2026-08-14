package com.java_web.backend.Common.DTO;

public class PromptTemplateDTO {
    private final String id;
    private final String module;
    private final String category;
    private final String name;
    private final String description;
    private final String prompt;

    public PromptTemplateDTO(String id,
                             String module,
                             String category,
                             String name,
                             String description,
                             String prompt) {
        this.id = id;
        this.module = module;
        this.category = category;
        this.name = name;
        this.description = description;
        this.prompt = prompt;
    }

    public String getId() {
        return id;
    }

    public String getModule() {
        return module;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPrompt() {
        return prompt;
    }
}
