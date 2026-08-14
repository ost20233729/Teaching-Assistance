package com.java_web.backend.Common.DTO;

import java.util.Map;

public class ContentVersionRestoreResponseDTO {
    private String moduleType;
    private Map<String, Object> data;

    public ContentVersionRestoreResponseDTO() {
    }

    public ContentVersionRestoreResponseDTO(String moduleType, Map<String, Object> data) {
        this.moduleType = moduleType;
        this.data = data;
    }

    public String getModuleType() {
        return moduleType;
    }

    public void setModuleType(String moduleType) {
        this.moduleType = moduleType;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
