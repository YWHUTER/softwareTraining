package com.campus.news.controller;

import com.campus.news.common.PageResult;
import com.campus.news.common.Result;
import com.campus.news.entity.Article;
import com.campus.news.entity.User;
import com.campus.news.service.FollowService;
import com.campus.news.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 关注模块控制器
 */
@Tag(name = "关注接口")
@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
public class FollowController {
    
    private final FollowService followService;
    private final UserService userService;
    
    /**
     * 关注/取消关注用户
     * POST /api/follow/{userId}
     */
    @Operation(summary = "关注/取消关注用户")
    @PostMapping("/{userId}")
    public Result<Map<String, Object>> toggleFollow(@PathVariable Long userId, Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        boolean isFollowing = followService.toggleFollow(currentUserId, userId);
        return Result.success(Map.of(
            "isFollowing", isFollowing,
            "message", isFollowing ? "关注成功" : "已取消关注"
        ));
    }
    
    /**
     * 检查是否已关注
     * GET /api/follow/check/{userId}
     */
    @Operation(summary = "检查是否已关注")
    @GetMapping("/check/{userId}")
    public Result<Boolean> checkFollow(@PathVariable Long userId, Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return Result.success(followService.isFollowing(currentUserId, userId));
    }
    
    /**
     * 获取我的关注列表
     * GET /api/follow/following
     */
    @Operation(summary = "获取我的关注列表")
    @GetMapping("/following")
    public Result<PageResult<User>> getFollowingList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return Result.success(followService.getFollowingList(currentUserId, current, size));
    }
    
    /**
     * 获取指定用户的关注列表
     * GET /api/follow/following/{userId}
     */
    @Operation(summary = "获取指定用户的关注列表")
    @GetMapping("/following/{userId}")
    public Result<PageResult<User>> getUserFollowingList(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long currentUserId = authentication != null ? getCurrentUserId(authentication) : null;
        PageResult<User> result = followService.getFollowingList(userId, current, size);
        // 标记当前用户是否关注了这些用户
        if (currentUserId != null) {
            for (User user : result.getRecords()) {
                user.setIsFollowed(followService.isFollowing(currentUserId, user.getId()));
            }
        }
        return Result.success(result);
    }
    
    /**
     * 获取我的粉丝列表
     * GET /api/follow/followers
     */
    @Operation(summary = "获取我的粉丝列表")
    @GetMapping("/followers")
    public Result<PageResult<User>> getFollowerList(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return Result.success(followService.getFollowerList(currentUserId, current, size, currentUserId));
    }
    
    /**
     * 获取指定用户的粉丝列表
     * GET /api/follow/followers/{userId}
     */
    @Operation(summary = "获取指定用户的粉丝列表")
    @GetMapping("/followers/{userId}")
    public Result<PageResult<User>> getUserFollowerList(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long currentUserId = authentication != null ? getCurrentUserId(authentication) : null;
        return Result.success(followService.getFollowerList(userId, current, size, currentUserId));
    }
    
    /**
     * 获取关注的人的文章动态
     * GET /api/follow/feed
     */
    @Operation(summary = "获取关注动态")
    @GetMapping("/feed")
    public Result<PageResult<Article>> getFollowingFeed(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return Result.success(followService.getFollowingArticles(currentUserId, current, size));
    }
    
    /**
     * 获取推荐关注的用户
     * GET /api/follow/recommend
     */
    @Operation(summary = "获取推荐关注用户")
    @GetMapping("/recommend")
    public Result<List<User>> getRecommendUsers(
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        return Result.success(followService.getRecommendUsers(currentUserId, limit));
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("username", username)
        );
        return user.getId();
    }
}
