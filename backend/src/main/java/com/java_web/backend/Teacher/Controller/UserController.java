package com.java_web.backend.Teacher.Controller;

import com.java_web.backend.Common.DTO.UpdateUserDTO;
import com.java_web.backend.Common.Entity.Restriction;
import com.java_web.backend.Common.Entity.User;
import com.java_web.backend.Common.Service.RestrictionService;
import com.java_web.backend.Teacher.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teacher")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private RestrictionService restrictionService;

    @GetMapping("/profile")
    public User getUserProfile(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return userService.getUserProfile(userId);
    }

    @PatchMapping("/profile")
    public User updateUserProfile(@RequestBody UpdateUserDTO dto, HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return userService.updateUserProfile(dto, userId);
    }

    @GetMapping("/restrictions")
    public List<Restriction> getCurrentUserRestrictions(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return restrictionService.getUserRestrictions(userId);
    }
}
