package com.java_web.backend.Common.Interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_web.backend.Common.DTO.ErrorResponse;
import com.java_web.backend.Common.Service.JWTService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private JWTService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        if (path.startsWith("/swagger-ui") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-resources") ||
                path.startsWith("/webjars")) {
            return true;
        }

        if (path.startsWith("/api/v1/auth/") || path.equals("/api/v1/llm/health")) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            writeError(response, 401, "未登录");
            return false;
        }

        try {
            Claims claims = jwtService.parseToken(token);
            String role = claims.get("role", String.class);

            if (path.startsWith("/api/v1/admin/") && !"admin".equals(role)) {
                writeError(response, 403, "无权访问该资源");
                return false;
            }

            if (path.startsWith("/api/v1/teacher/") && !"teacher".equals(role)) {
                writeError(response, 403, "无权访问该资源");
                return false;
            }

            Integer userId = ((Number) claims.get("id")).intValue();
            request.setAttribute("userId", userId);
            request.setAttribute("username", claims.getSubject());
            request.setAttribute("userRole", role);
            return true;
        } catch (Exception exception) {
            writeError(response, 401, "登录已过期，请重新登录");
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(status, message));
    }
}
