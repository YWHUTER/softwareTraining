package com.campus.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.news.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
