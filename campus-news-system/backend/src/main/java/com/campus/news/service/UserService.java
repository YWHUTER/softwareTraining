package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.news.common.PageResult;
import com.campus.news.dto.LoginRequest;
import com.campus.news.dto.RegisterRequest;
import com.campus.news.entity.College;
import com.campus.news.entity.Role;
import com.campus.news.entity.User;
import com.campus.news.entity.UserRole;
import com.campus.news.exception.BusinessException;
import com.campus.news.mapper.UserMapper;
import com.campus.news.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> implements UserDetailsService {
    
    private final UserMapper userMapper;
    private final RoleService roleService;
    private final UserRoleService userRoleService;
    private final CollegeService collegeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        
        List<Role> roles = roleService.getRolesByUserId(user.getId());
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName()))
                .collect(Collectors.toList());
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
    
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        if (userMapper.selectOne(new QueryWrapper<User>().eq("username", request.getUsername())) != null) {
            throw new BusinessException("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setCollegeId(request.getCollegeId());
        user.setStudentId(request.getStudentId());
        user.setStatus(1);
        
        userMapper.insert(user);
        
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(request.getRoleId());
        userRoleService.save(userRole);
        
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", getUserInfo(user.getId()));
        return result;
    }
    
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 检查用户是否被禁用
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        
        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", getUserInfo(user.getId()));
        return result;
    }
    
    public User getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        user.setRoles(roleService.getRolesByUserId(userId));
        if (user.getCollegeId() != null) {
            user.setCollege(collegeService.getById(user.getCollegeId()));
        }
        
        return user;
    }
    
    public PageResult<User> getUserList(Long current, Long size, String keyword, Long collegeId, Integer status) {
        Page<User> page = new Page<>(current, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("username", keyword)
                    .or().like("real_name", keyword)
                    .or().like("email", keyword);
        }
        if (collegeId != null) {
            wrapper.eq("college_id", collegeId);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        
        Page<User> resultPage = userMapper.selectPage(page, wrapper);
        resultPage.getRecords().forEach(user -> {
            user.setRoles(roleService.getRolesByUserId(user.getId()));
            if (user.getCollegeId() != null) {
                user.setCollege(collegeService.getById(user.getCollegeId()));
            }
        });
        
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords(), 
                resultPage.getCurrent(), resultPage.getSize());
    }
    
    @Transactional
    public boolean updateUserStatus(Long userId, Integer status) {
        return userMapper.update(null,
            new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<User>()
                .eq("id", userId)
                .set("status", status)
        ) > 0;
    }
}
