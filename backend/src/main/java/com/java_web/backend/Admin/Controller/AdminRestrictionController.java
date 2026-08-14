package com.java_web.backend.Admin.Controller;

import com.java_web.backend.Admin.Service.AdminRestrictionService;
import com.java_web.backend.Common.Entity.Restriction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users/{userId}/restrictions")
public class AdminRestrictionController {
    @Autowired
    private AdminRestrictionService adminRestrictionService;

    @PostMapping
    public ResponseEntity<Restriction> addRestriction(@PathVariable Integer userId, @RequestBody Restriction restriction) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminRestrictionService.addRestriction(userId, restriction));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeRestriction(@PathVariable Integer userId, @PathVariable Integer id) {
        adminRestrictionService.removeRestriction(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<Restriction> getUserRestrictions(@PathVariable Integer userId) {
        return adminRestrictionService.getUserRestrictions(userId);
    }
}
