package com.java_web.backend.service;

import com.java_web.backend.Common.DTO.LoginDTO;
import com.java_web.backend.Common.Entity.User;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.UserMapper;
import com.java_web.backend.Common.Service.JWTService;
import com.java_web.backend.Teacher.Service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private UserService userService;

    @Test
    void login_ShouldUseUsernameAndReturnToken() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("teacher01");
        dto.setPassword("123456");

        User user = new User();
        user.setId(2);
        user.setUsername("teacher01");
        user.setPassword("123456");
        user.setRole("teacher");
        user.setStatus("active");

        when(userMapper.findByUsername("teacher01")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("mock-token");

        Map<String, Object> result = userService.login(dto);

        assertEquals("mock-token", result.get("token"));
        assertEquals(2, result.get("userId"));
        assertEquals("teacher01", result.get("username"));
        assertEquals("teacher", result.get("role"));
    }

    @Test
    void login_ShouldRejectBlankUsername() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("   ");
        dto.setPassword("123456");

        ApiException exception = assertThrows(ApiException.class, () -> userService.login(dto));

        assertEquals(400, exception.getStatus().value());
        assertEquals("用户名和密码不能为空", exception.getMessage());
    }
}
