package com.campus.news.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.entity.UserRole;
import com.campus.news.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;

@Service
public class UserRoleService extends ServiceImpl<UserRoleMapper, UserRole> {
}
