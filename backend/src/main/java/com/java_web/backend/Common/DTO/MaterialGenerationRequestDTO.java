package com.java_web.backend.Common.DTO;

public class MaterialGenerationRequestDTO {
    private String courseTitle;
    private String request;

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }
}
