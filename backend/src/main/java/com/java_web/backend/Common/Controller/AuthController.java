package com.java_web.backend.Common.Controller;

import com.java_web.backend.Admin.Service.AdminUserService;
import com.java_web.backend.Common.DTO.AdminLoginDTO;
import com.java_web.backend.Common.DTO.LoginDTO;
import com.java_web.backend.Common.DTO.MessageResponse;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Service.SignupService;
import com.java_web.backend.Teacher.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private SignupService signupService;

    @PostMapping("/teacher-sessions")
    public ResponseEntity<Map<String, Object>> createTeacherSession(@RequestBody LoginDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.login(dto));
    }

    @PostMapping("/admin-sessions")
    public ResponseEntity<Map<String, Object>> createAdminSession(@RequestBody AdminLoginDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.login(dto));
    }

    @PostMapping("/registrations")
    public ResponseEntity<MessageResponse> register(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        String email = payload.get("email");
        String role = payload.getOrDefault("role", "teacher");

        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {
            throw ApiException.badRequest("用户名、密码和邮箱不能为空");
        }

        signupService.register(username.trim(), password, email.trim(), role);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse("注册成功，请登录"));
    }

    @GetMapping("/usernames/availability")
    public Map<String, Object> checkUsernameAvailable(@RequestParam String username) {
        Map<String, Object> result = new HashMap<>();
        result.put("available", signupService.isUsernameAvailable(username));
        return result;
    }

    @GetMapping("/emails/availability")
    public Map<String, Object> checkEmailAvailable(@RequestParam String email) {
        Map<String, Object> result = new HashMap<>();
        result.put("available", signupService.isEmailAvailable(email));
        return result;
    }
}
