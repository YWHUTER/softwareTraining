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
    
    public List<Role> getRolesByUserId(Long userId) {
        return roleMapper.findByUserId(userId);
    }
}
