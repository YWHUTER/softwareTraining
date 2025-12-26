package com.campus.news.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.entity.Role;
import com.campus.news.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService extends ServiceImpl<RoleMapper, Role> {
    
    private final RoleMapper roleMapper;
    // 根据用户id获取角色列表
    public List<Role> getRolesByUserId(Long userId) {
        return roleMapper.findByUserId(userId);
    }
}
