package com.java_web.backend.Teacher.Service;

import com.java_web.backend.Common.DTO.LoginDTO;
import com.java_web.backend.Common.DTO.UpdateUserDTO;
import com.java_web.backend.Common.Entity.User;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.UserMapper;
import com.java_web.backend.Common.Service.JWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JWTService jwtService;

    public Map<String, Object> login(LoginDTO dto) {
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty() ||
                dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw ApiException.badRequest("用户名和密码不能为空");
        }

        User user = userMapper.findByUsername(dto.getUsername().trim());
        if (user == null) {
            throw ApiException.unauthorized("用户不存在");
        }

        if (!dto.getPassword().equals(user.getPassword())) {
            throw ApiException.unauthorized("密码错误");
        }

        if (!"teacher".equals(user.getRole())) {
            throw ApiException.forbidden("当前账号不是教师账号");
        }

        if (!"active".equals(user.getStatus())) {
            throw ApiException.forbidden("账号已被冻结");
        }

        String token = jwtService.generateToken(user);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        return result;
    }

    public User getUserProfile(Integer userId) {
        User user = requireUser(userId);
        user.setPassword(null);
        return user;
    }

    public User updateUserProfile(UpdateUserDTO dto, Integer userId) {
        User user = requireUser(userId);

        if (dto.getUsername() != null && !dto.getUsername().trim().isEmpty()) {
            User existingUser = userMapper.findByUsername(dto.getUsername().trim());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw ApiException.conflict("用户名已被占用");
            }
            user.setUsername(dto.getUsername().trim());
        }

        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            User existingUser = userMapper.findByEmail(dto.getEmail().trim());
            if (existingUser != null && !existingUser.getId().equals(userId)) {
                throw ApiException.conflict("邮箱已被占用");
            }
            user.setEmail(dto.getEmail().trim());
        }

        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }

        user.setUpdatedAt(new Date());
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    private User requireUser(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw ApiException.notFound("用户不存在");
        }
        return user;
    }
}
