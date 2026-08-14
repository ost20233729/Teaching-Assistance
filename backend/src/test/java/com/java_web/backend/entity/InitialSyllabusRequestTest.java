package com.java_web.backend.entity;

import com.java_web.backend.Common.Entity.InitialSyllabusRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitialSyllabusRequestTest {

    @Test
    void defaultConstructor_InitializesDefaults() {
        InitialSyllabusRequest request = new InitialSyllabusRequest();

        assertNotNull(request);
        assertNull(request.getCourseId());
        assertEquals("XXXX", request.getCourseCode());
        assertEquals("中文", request.getTeachingLanguage());
        assertEquals("XXXX", request.getResponsibleCollege());
        assertEquals("专业必修", request.getCourseCategory());
        assertEquals("X老师", request.getPrinciple());
        assertEquals("X老师", request.getVerifier());
        assertEquals("3", request.getCredit());
        assertEquals("48", request.getCourseHour());
        assertNull(request.getCourseIntroduction());
        assertNull(request.getTeachingTarget());
        assertEquals("考试", request.getEvaluationMode());
        assertEquals("否", request.getWhetherTechnicalCourse());
        assertEquals("理论", request.getAssessmentType());
        assertEquals("百分制", request.getGradeRecording());
    }

    @Test
    void convenienceConstructor_RetainsPassedValuesAndDefaults() {
        InitialSyllabusRequest request = new InitialSyllabusRequest("CS001", "计算机网络", "中文", "生成课程讲义");

        assertEquals("CS001", request.getCourseId());
        assertEquals("计算机网络", request.getCourseTitle());
        assertEquals("中文", request.getTeachingLanguage());
        assertEquals("生成课程讲义", request.getRequest());
        assertEquals("XXXX", request.getCourseCode());
    }

    @Test
    void resetToDefaults_ResetsDefaultFields() {
        InitialSyllabusRequest request = new InitialSyllabusRequest();
        request.setCourseCode("TEMP");
        request.setResponsibleCollege("TEMP");
        request.setCourseIntroduction("intro");

        request.resetToDefaults();

        assertEquals("XXXX", request.getCourseCode());
        assertEquals("XXXX", request.getResponsibleCollege());
        assertEquals("intro", request.getCourseIntroduction());
        assertTrue(request.toString().contains("courseId='null'"));
    }
}
