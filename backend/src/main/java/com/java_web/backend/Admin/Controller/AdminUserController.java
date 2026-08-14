package com.java_web.backend.Admin.Controller;

import com.java_web.backend.Admin.Service.AdminUserService;
import com.java_web.backend.Common.DTO.UpdateUserDTO;
import com.java_web.backend.Common.Entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    @Autowired
    private AdminUserService adminUserService;

    @GetMapping
    public List<User> getUserList() {
        return adminUserService.listUsers();
    }

    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.addUser(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public User getUserInfo(@PathVariable Integer id) {
        return adminUserService.getUserInfo(id);
    }

    @PatchMapping("/{id}")
    public User updateUser(@PathVariable Integer id, @RequestBody UpdateUserDTO request) {
        return adminUserService.updateUser(id, request);
    }
}
