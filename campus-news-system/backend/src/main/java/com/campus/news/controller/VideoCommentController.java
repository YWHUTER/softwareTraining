package com.campus.news.controller;

import com.campus.news.common.PageResult;
import com.campus.news.common.Result;
import com.campus.news.entity.VideoComment;
import com.campus.news.service.VideoCommentService;
import com.campus.news.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "视频评论接口")
@RestController
@RequestMapping("/video/comment")
@RequiredArgsConstructor
public class VideoCommentController {
    
    private final VideoCommentService commentService;
    private final UserService userService;
    
    @Operation(summary = "发表评论")
    @PostMapping("/create")
    public Result<VideoComment> createComment(@RequestBody Map<String, Object> params,
                                               Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        Long videoId = Long.valueOf(params.get("videoId").toString());
        String content = params.get("content").toString();
        Long parentId = params.get("parentId") != null ? 
                Long.valueOf(params.get("parentId").toString()) : null;
        
        return Result.success(commentService.createComment(videoId, content, parentId, userId));
    }
    
    @Operation(summary = "获取视频评论列表")
    @GetMapping("/list/{videoId}")
    public Result<PageResult<VideoComment>> getComments(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "hot") String sortBy,
            Authentication authentication) {
        Long userId = authentication != null ? getCurrentUserId(authentication) : null;
        return Result.success(commentService.getComments(videoId, current, size, sortBy, userId));
    }
    
    @Operation(summary = "获取评论的回复")
    @GetMapping("/replies/{commentId}")
    public Result<List<VideoComment>> getReplies(
            @PathVariable Long commentId,
            Authentication authentication) {
        Long userId = authentication != null ? getCurrentUserId(authentication) : null;
        return Result.success(commentService.getAllReplies(commentId, userId));
    }
    
    @Operation(summary = "删除评论")
    @DeleteMapping("/delete/{commentId}")
    public Result<Boolean> deleteComment(@PathVariable Long commentId,
                                          Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(commentService.deleteComment(commentId, userId));
    }
    
    @Operation(summary = "点赞/取消点赞评论")
    @PostMapping("/like/{commentId}")
    public Result<Boolean> toggleLike(@PathVariable Long commentId,
                                       Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(commentService.toggleCommentLike(commentId, userId));
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        com.campus.news.entity.User userEntity = userService.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.campus.news.entity.User>()
                .eq("username", username));
        return userEntity.getId();
    }
}
