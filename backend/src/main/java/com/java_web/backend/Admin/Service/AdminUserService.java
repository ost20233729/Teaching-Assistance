package com.java_web.backend.Admin.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.java_web.backend.Common.DTO.AdminLoginDTO;
import com.java_web.backend.Common.DTO.UpdateUserDTO;
import com.java_web.backend.Common.Entity.User;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.UserMapper;
import com.java_web.backend.Common.Service.JWTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminUserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JWTService jwtService;

    public Map<String, Object> login(AdminLoginDTO loginDTO) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", loginDTO.getUsername())
                .eq("role", "admin")
                .eq("is_deleted", 0);
        User admin = userMapper.selectOne(queryWrapper);

        if (admin == null) {
            throw ApiException.unauthorized("管理员账号不存在");
        }

        if (!admin.getPassword().equals(loginDTO.getPassword())) {
            throw ApiException.unauthorized("密码错误");
        }

        String token = jwtService.generateToken(admin);
        admin.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", admin);
        return result;
    }

    public List<User> listUsers() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("role", "teacher")
                .eq("is_deleted", 0);
        List<User> users = userMapper.selectList(queryWrapper);
        users.forEach(user -> user.setPassword(null));
        return users;
    }

    public User addUser(User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw ApiException.badRequest("用户名不能为空");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw ApiException.badRequest("邮箱不能为空");
        }

        if (userMapper.findByUsername(user.getUsername().trim()) != null) {
            throw ApiException.conflict("用户名已被使用");
        }

        if (userMapper.findByEmail(user.getEmail().trim()) != null) {
            throw ApiException.conflict("邮箱已被注册");
        }

        user.setUsername(user.getUsername().trim());
        user.setEmail(user.getEmail().trim());
        user.setRole("teacher");
        user.setIsDeleted(0);
        user.setPassword(user.getPassword() == null || user.getPassword().trim().isEmpty() ? "123456" : user.getPassword());
        user.setStatus(user.getStatus() == null || user.getStatus().trim().isEmpty() ? "active" : user.getStatus());
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    public void deleteUser(Integer id) {
        User user = requireUser(id);
        user.setIsDeleted(1);
        user.setUpdatedAt(new Date());
        userMapper.updateById(user);
    }

    public User getUserInfo(Integer id) {
        User user = requireUser(id);
        user.setPassword(null);
        return user;
    }

    public User updateUser(Integer id, UpdateUserDTO request) {
        User user = requireUser(id);

        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            User existingUser = userMapper.findByUsername(request.getUsername().trim());
            if (existingUser != null && !existingUser.getId().equals(id)) {
                throw ApiException.conflict("用户名已被占用");
            }
            user.setUsername(request.getUsername().trim());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            User existingUser = userMapper.findByEmail(request.getEmail().trim());
            if (existingUser != null && !existingUser.getId().equals(id)) {
                throw ApiException.conflict("邮箱已被占用");
            }
            user.setEmail(request.getEmail().trim());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        user.setUpdatedAt(new Date());
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    private User requireUser(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw ApiException.notFound("用户不存在");
        }
        return user;
    }
}
