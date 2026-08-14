package com.java_web.backend.Common.DTO;

public class CourseExportDTO {
    private final String fileName;
    private final String content;

    public CourseExportDTO(String fileName, String content) {
        this.fileName = fileName;
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContent() {
        return content;
    }
}
