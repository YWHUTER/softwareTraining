package com.campus.news.controller;

import com.campus.news.common.Result;
import com.campus.news.dto.CommentCreateRequest;
import com.campus.news.entity.Comment;
import com.campus.news.service.CommentService;
import com.campus.news.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "评论接口")
@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    private final UserService userService;
    
    @Operation(summary = "创建评论")
    @PostMapping("/create")
    public Result<Comment> createComment(@Valid @RequestBody CommentCreateRequest request,
                                         Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(commentService.createComment(request, userId));
    }
    
    @Operation(summary = "获取评论列表")
    @GetMapping("/list")
    public Result<List<Comment>> getCommentList(@RequestParam Long articleId) {
        return Result.success(commentService.getCommentList(articleId));
    }
    
    @Operation(summary = "删除评论")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteComment(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(commentService.deleteComment(id, userId));
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        com.campus.news.entity.User user = userService.getOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.campus.news.entity.User>()
                .eq("username", username));
        return user.getId();
    }
}
