package com.campus.news.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.news.common.Result;
import com.campus.news.entity.Notification;
import com.campus.news.entity.User;
import com.campus.news.service.NotificationService;
import com.campus.news.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    private final UserService userService;
    
    /**
     * 获取通知列表
     */
    @GetMapping("/list")
    public Result<Page<Notification>> getNotifications(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Page<Notification> page = notificationService.getNotifications(userId, current, size);
        return Result.success(page);
    }
    
    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Long count = notificationService.getUnreadCount(userId);
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }
    
    /**
     * 标记单个通知为已读
     */
    @PutMapping("/read/{id}")
    public Result<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.markAsRead(id, userId);
        return Result.success();
    }
    
    /**
     * 标记所有通知为已读
     */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.markAllAsRead(userId);
        return Result.success();
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getOne(new QueryWrapper<User>().eq("username", username));
        return user.getId();
    }
}
