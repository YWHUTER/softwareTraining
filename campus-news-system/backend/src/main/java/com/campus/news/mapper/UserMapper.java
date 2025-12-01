package com.campus.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.news.entity.User;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);
}
