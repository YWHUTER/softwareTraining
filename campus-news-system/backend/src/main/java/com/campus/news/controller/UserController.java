package com.campus.news.controller;

import com.campus.news.common.PageResult;
import com.campus.news.common.Result;
import com.campus.news.entity.User;
import com.campus.news.service.UserService;
import com.campus.news.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final JwtUtil jwtUtil;
    
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<User> getUserInfo(Authentication authentication) {
        String username = authentication.getName();
        Long userId = userService.getUserIdByUsername(username);
        return Result.success(userService.getUserInfo(userId));
    }
    
    @Operation(summary = "获取用户列表")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public Result<PageResult<User>> getUserList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.getUserList(current, size, keyword, collegeId, status));
    }
    
    @Operation(summary = "更新用户状态")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/status/{userId}")
    public Result<Boolean> updateUserStatus(@PathVariable Long userId, @RequestParam Integer status) {
        return Result.success(userService.updateUserStatus(userId, status));
    }
    
    @Operation(summary = "搜索用户（用于@提及）")
    @GetMapping("/search")
    public Result<PageResult<User>> searchUsers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.getUserList(current, size, keyword, null, 1));
    }
    
    @Operation(summary = "更新用户头像")
    @PutMapping("/avatar")
    public Result<User> updateAvatar(@RequestParam String avatar, Authentication authentication) {
        String username = authentication.getName();
        Long userId = userService.getUserIdByUsername(username);
        userService.updateAvatar(userId, avatar);
        return Result.success(userService.getUserInfo(userId));
    }
    
    @Operation(summary = "更新用户信息")
    @PutMapping("/update")
    public Result<User> updateUserInfo(@RequestBody User updateUser, Authentication authentication) {
        String username = authentication.getName();
        Long userId = userService.getUserIdByUsername(username);
        userService.updateUserInfo(userId, updateUser);
        return Result.success(userService.getUserInfo(userId));
    }
}
