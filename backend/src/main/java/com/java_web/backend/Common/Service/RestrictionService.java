package com.java_web.backend.Common.Service;

import com.java_web.backend.Common.Entity.Restriction;
import com.java_web.backend.Common.Exception.ApiException;
import com.java_web.backend.Common.Mapper.RestrictionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestrictionService {
    @Autowired
    private RestrictionMapper restrictionMapper;

    public void ensureFunctionAvailable(Integer userId, String functionName) {
        Restriction restriction = restrictionMapper.selectByUserIdAndFunction(userId, functionName);
        if (restriction != null) {
            throw ApiException.forbidden("当前账号已被限制使用该功能");
        }
    }

    public List<Restriction> getUserRestrictions(Integer userId) {
        return restrictionMapper.selectByUserId(userId);
    }
}
