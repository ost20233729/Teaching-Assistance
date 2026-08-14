package com.java_web.backend.Common.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.java_web.backend.Common.Entity.Courseware;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CoursewareMapper extends BaseMapper<Courseware> {
    @Select("SELECT * FROM courseware WHERE course_id = #{courseId}")
    Courseware selectByCourseId(Integer courseId);
}
