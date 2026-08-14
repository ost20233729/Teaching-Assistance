package com.java_web.backend.Common.Service;

import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Entity.User;
import com.java_web.backend.Common.Mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SignupService {
    @Autowired
    private UserMapper userMapper;

    public void register(String username, String password, String email, String role) {
        if (userMapper.findByUsername(username) != null) {
            throw ApiException.conflict("用户名已被使用");
        }

        if (userMapper.findByEmail(email) != null) {
            throw ApiException.conflict("邮箱已被注册");
        }

        if (!"teacher".equals(role) && !"admin".equals(role)) {
            throw ApiException.badRequest("无效的用户角色");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus("active");
        user.setIsDeleted(0);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        userMapper.insert(user);
    }

    public boolean isUsernameAvailable(String username) {
        return userMapper.findByUsername(username) == null;
    }

    public boolean isEmailAvailable(String email) {
        return userMapper.findByEmail(email) == null;
    }
}
