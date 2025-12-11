package com.campus.news.controller;

import com.campus.news.common.Result;
import com.campus.news.dto.VideoCreateRequest;
import com.campus.news.entity.Video;
import com.campus.news.service.UserService;
import com.campus.news.service.VideoService;
import com.campus.news.service.VideoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "视频上传接口")
@RestController
@RequestMapping("/video/upload")
@RequiredArgsConstructor
public class VideoUploadController {
    
    private final VideoStorageService videoStorageService;
    private final VideoService videoService;
    private final UserService userService;
    
    @Operation(summary = "上传视频文件")
    @PostMapping("/file")
    public Result<Map<String, String>> uploadVideoFile(@RequestParam("file") MultipartFile file) {
        String videoUrl = videoStorageService.uploadVideo(file);
        
        Map<String, String> result = new HashMap<>();
        result.put("videoUrl", videoUrl);
        result.put("fileName", file.getOriginalFilename());
        result.put("fileSize", String.valueOf(file.getSize()));
        
        return Result.success(result);
    }
    
    @Operation(summary = "上传视频缩略图")
    @PostMapping("/thumbnail")
    public Result<Map<String, String>> uploadThumbnail(@RequestParam("file") MultipartFile file) {
        String thumbnailUrl = videoStorageService.uploadThumbnail(file);
        
        Map<String, String> result = new HashMap<>();
        result.put("thumbnailUrl", thumbnailUrl);
        
        return Result.success(result);
    }
    
    @Operation(summary = "完整上传视频(文件+信息)")
    @PostMapping("/complete")
    public Result<Video> uploadComplete(
            @RequestParam("video") MultipartFile videoFile,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnailFile,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "channelName", required = false) String channelName,
            @RequestParam(value = "duration", required = false) String duration,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            Authentication authentication) {
        
        Long userId = getCurrentUserId(authentication);
        
        // 上传视频文件
        String videoUrl = videoStorageService.uploadVideo(videoFile);
        
        // 上传缩略图
        String thumbnailUrl = null;
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            thumbnailUrl = videoStorageService.uploadThumbnail(thumbnailFile);
        }
        
        // 创建视频记录
        VideoCreateRequest request = new VideoCreateRequest();
        request.setTitle(title);
        request.setDescription(description);
        request.setVideoUrl(videoUrl);
        request.setThumbnail(thumbnailUrl);
        request.setCategoryId(categoryId);
        request.setChannelName(channelName);
        request.setDuration(duration);
        request.setDurationSeconds(durationSeconds);
        request.setFileSize(videoFile.getSize());
        
        Video video = videoService.createVideo(request, userId);
        
        return Result.success(video);
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        com.campus.news.entity.User userEntity = userService.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.campus.news.entity.User>()
                .eq("username", username));
        return userEntity.getId();
    }
}
