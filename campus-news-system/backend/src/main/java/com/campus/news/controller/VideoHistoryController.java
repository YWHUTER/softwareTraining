package com.campus.news.controller;

import com.campus.news.common.PageResult;
import com.campus.news.common.Result;
import com.campus.news.entity.Video;
import com.campus.news.service.UserService;
import com.campus.news.service.VideoHistoryService;
import com.campus.news.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "视频历史记录接口")
@RestController
@RequestMapping("/video/history")
@RequiredArgsConstructor
public class VideoHistoryController {
    
    private final VideoHistoryService historyService;
    private final VideoService videoService;
    private final UserService userService;
    
    @Operation(summary = "添加观看历史")
    @PostMapping("/add/{videoId}")
    public Result<Boolean> addHistory(@PathVariable Long videoId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        historyService.addHistory(userId, videoId);
        return Result.success(true);
    }
    
    @Operation(summary = "获取观看历史")
    @GetMapping("/list")
    public Result<PageResult<Video>> getHistory(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "12") Integer size,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        
        List<Long> videoIds = historyService.getHistory(userId, current, size);
        long total = historyService.getHistoryCount(userId);
        
        if (videoIds.isEmpty()) {
            return Result.success(new PageResult<>(0L, List.of(), (long) current, (long) size));
        }
        
        List<Video> videos = videoService.getVideosByIds(videoIds, userId);
        return Result.success(new PageResult<>(total, videos, (long) current, (long) size));
    }
    
    @Operation(summary = "删除单条历史记录")
    @DeleteMapping("/remove/{videoId}")
    public Result<Boolean> removeHistory(@PathVariable Long videoId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        historyService.removeHistory(userId, videoId);
        return Result.success(true);
    }
    
    @Operation(summary = "清空观看历史")
    @DeleteMapping("/clear")
    public Result<Boolean> clearHistory(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        historyService.clearHistory(userId);
        return Result.success(true);
    }
    
    @Operation(summary = "标记/取消不喜欢")
    @PostMapping("/dislike/{videoId}")
    public Result<Boolean> toggleDislike(@PathVariable Long videoId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        
        if (historyService.isDisliked(userId, videoId)) {
            historyService.removeDislike(userId, videoId);
            return Result.success(false);
        } else {
            historyService.addDislike(userId, videoId);
            return Result.success(true);
        }
    }
    
    @Operation(summary = "检查是否不喜欢")
    @GetMapping("/dislike/check/{videoId}")
    public Result<Boolean> checkDislike(@PathVariable Long videoId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(historyService.isDisliked(userId, videoId));
    }
    
    @Operation(summary = "添加到稍后观看")
    @PostMapping("/watchlater/{videoId}")
    public Result<Boolean> toggleWatchLater(@PathVariable Long videoId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        
        if (historyService.isInWatchLater(userId, videoId)) {
            historyService.removeWatchLater(userId, videoId);
            return Result.success(false);
        } else {
            historyService.addWatchLater(userId, videoId);
            return Result.success(true);
        }
    }
    
    @Operation(summary = "获取稍后观看列表")
    @GetMapping("/watchlater/list")
    public Result<PageResult<Video>> getWatchLater(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "12") Integer size,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        
        List<Long> videoIds = historyService.getWatchLater(userId, current, size);
        
        if (videoIds.isEmpty()) {
            return Result.success(new PageResult<>(0L, List.of(), (long) current, (long) size));
        }
        
        List<Video> videos = videoService.getVideosByIds(videoIds, userId);
        return Result.success(new PageResult<>((long) videos.size(), videos, (long) current, (long) size));
    }
    
    @Operation(summary = "保存观看进度")
    @PostMapping("/progress/{videoId}")
    public Result<Boolean> saveProgress(
            @PathVariable Long videoId,
            @RequestBody Map<String, Integer> body,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Integer seconds = body.get("seconds");
        if (seconds != null) {
            historyService.saveProgress(userId, videoId, seconds);
        }
        return Result.success(true);
    }
    
    @Operation(summary = "获取观看进度")
    @GetMapping("/progress/{videoId}")
    public Result<Integer> getProgress(@PathVariable Long videoId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(historyService.getProgress(userId, videoId));
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        com.campus.news.entity.User userEntity = userService.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.campus.news.entity.User>()
                .eq("username", username));
        return userEntity.getId();
    }
}
