package com.java_web.backend.Common.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.java_web.backend.Common.Entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
    @Select("select * from notification where user_id = #{userId} order by is_read asc, created_at desc, id desc")
    List<Notification> selectByUserId(Integer userId);

    @Select("select * from notification where user_id = #{userId} and id = #{notificationId}")
    Notification selectByUserIdAndId(@Param("userId") Integer userId, @Param("notificationId") Long notificationId);
}
