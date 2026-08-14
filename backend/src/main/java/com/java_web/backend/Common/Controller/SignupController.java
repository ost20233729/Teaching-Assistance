package com.java_web.backend.Common.Controller;

import com.java_web.backend.Common.Service.SignupService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/signup")
public class SignupController {
    @Autowired
    private SignupService signupService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        String email = payload.get("email");
        String role = payload.get("role");

        if (username == null || username.trim().isEmpty()
                || password == null || password.trim().isEmpty()
                || email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("用户名、密码和邮箱不能为空");
        }

        if (role == null || role.trim().isEmpty()) {
            role = "teacher";
        }

        signupService.register(username, password, email, role);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "注册成功，请登录");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsernameAvailable(@RequestParam String username) {
        boolean isAvailable = signupService.isUsernameAvailable(username);
        Map<String, Object> result = new HashMap<>();
        result.put("available", isAvailable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailAvailable(@RequestParam String email) {
        boolean isAvailable = signupService.isEmailAvailable(email);
        Map<String, Object> result = new HashMap<>();
        result.put("available", isAvailable);
        return ResponseEntity.ok(result);
    }
}
