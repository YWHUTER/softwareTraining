package com.campus.news.controller;

import com.campus.news.common.PageResult;
import com.campus.news.common.Result;
import com.campus.news.dto.VideoCreateRequest;
import com.campus.news.dto.VideoQueryRequest;
import com.campus.news.entity.Video;
import com.campus.news.entity.VideoCategory;
import com.campus.news.service.VideoCategoryService;
import com.campus.news.service.VideoLikeService;
import com.campus.news.service.VideoService;
import com.campus.news.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "视频接口")
@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoController {
    
    private final VideoService videoService;
    private final VideoLikeService videoLikeService;
    private final VideoCategoryService videoCategoryService;
    private final UserService userService;
    
    @Operation(summary = "上传视频信息")
    @PostMapping("/create")
    public Result<Video> createVideo(@Valid @RequestBody VideoCreateRequest request,
                                     Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(videoService.createVideo(request, userId));
    }
    
    @Operation(summary = "获取视频列表")
    @GetMapping("/list")
    public Result<PageResult<Video>> getVideoList(VideoQueryRequest request,
                                                   Authentication authentication) {
        Long userId = authentication != null ? getCurrentUserId(authentication) : null;
        return Result.success(videoService.getVideoList(request, userId));
    }
    
    @Operation(summary = "获取视频详情")
    @GetMapping("/detail/{id}")
    public Result<Video> getVideoDetail(@PathVariable Long id, Authentication authentication) {
        Long userId = authentication != null ? getCurrentUserId(authentication) : null;
        return Result.success(videoService.getVideoDetail(id, userId));
    }
    
    @Operation(summary = "更新视频信息")
    @PutMapping("/update/{id}")
    public Result<Boolean> updateVideo(@PathVariable Long id,
                                       @Valid @RequestBody VideoCreateRequest request,
                                       Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(videoService.updateVideo(id, request, userId));
    }
    
    @Operation(summary = "删除视频")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteVideo(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(videoService.deleteVideo(id, userId));
    }
    
    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/like/{id}")
    public Result<Boolean> toggleLike(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(videoLikeService.toggleLike(id, userId));
    }
    
    @Operation(summary = "审核视频")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/approve/{id}")
    public Result<Boolean> approveVideo(@PathVariable Long id, @RequestParam Integer isApproved) {
        return Result.success(videoService.approveVideo(id, isApproved));
    }
    
    @Operation(summary = "获取视频分类列表")
    @GetMapping("/categories")
    public Result<List<VideoCategory>> getCategories() {
        return Result.success(videoCategoryService.getAllCategories());
    }
    
    @Operation(summary = "获取热门视频")
    @GetMapping("/hot")
    public Result<List<Video>> getHotVideos(@RequestParam(defaultValue = "10") Integer count) {
        return Result.success(videoService.getHotVideos(count));
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        com.campus.news.entity.User userEntity = userService.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.campus.news.entity.User>()
                .eq("username", username));
        return userEntity.getId();
    }
}
