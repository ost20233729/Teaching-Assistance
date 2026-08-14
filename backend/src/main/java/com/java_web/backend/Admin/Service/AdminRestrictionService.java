package com.java_web.backend.Admin.Service;

import com.java_web.backend.Common.Entity.Restriction;
import com.java_web.backend.Common.Entity.User;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.RestrictionMapper;
import com.java_web.backend.Common.Mapper.UserMapper;
import com.java_web.backend.Common.Service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AdminRestrictionService {
    @Autowired
    private RestrictionMapper restrictionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    public Restriction addRestriction(Integer userId, Restriction restriction) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw ApiException.notFound("用户不存在");
        }

        if (restriction.getFunctionName() == null || restriction.getFunctionName().trim().isEmpty()) {
            throw ApiException.badRequest("功能标识不能为空");
        }

        Restriction existingRestriction = restrictionMapper.selectByUserIdAndFunction(userId, restriction.getFunctionName().trim());
        if (existingRestriction != null) {
            throw ApiException.conflict("该功能限制已存在");
        }

        restriction.setUserId(userId);
        restriction.setFunctionName(restriction.getFunctionName().trim());
        restriction.setCreatedAt(new Date());
        restrictionMapper.insert(restriction);
        notificationService.createRestrictionNotification(userId, restriction.getFunctionName(), true);
        return restriction;
    }

    public void removeRestriction(Integer userId, Integer id) {
        Restriction restriction = restrictionMapper.selectById(id);
        if (restriction == null || !restriction.getUserId().equals(userId)) {
            throw ApiException.notFound("限制记录不存在");
        }
        restrictionMapper.deleteById(id);
        notificationService.createRestrictionNotification(userId, restriction.getFunctionName(), false);
    }

    public List<Restriction> getUserRestrictions(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw ApiException.notFound("用户不存在");
        }
        return restrictionMapper.selectByUserId(userId);
    }
}
