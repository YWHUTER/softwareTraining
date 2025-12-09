package com.campus.news.controller;

import com.campus.news.common.PageResult;
import com.campus.news.common.Result;
import com.campus.news.dto.ArticleCreateRequest;
import com.campus.news.dto.ArticleQueryRequest;
import com.campus.news.entity.Article;
import com.campus.news.service.ArticleLikeService;
import com.campus.news.service.ArticleFavoriteService;
import com.campus.news.service.ArticleService;
import com.campus.news.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "文章接口")
@RestController
@RequestMapping("/article")
@RequiredArgsConstructor
public class ArticleController {
    
    private final ArticleService articleService;
    private final ArticleLikeService articleLikeService;
    private final ArticleFavoriteService articleFavoriteService;
    private final UserService userService;
    
    @Operation(summary = "创建文章")
    @PostMapping("/create")
    public Result<Article> createArticle(@Valid @RequestBody ArticleCreateRequest request,
                                         Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(articleService.createArticle(request, userId));
    }
    
    @Operation(summary = "获取文章列表")
    @GetMapping("/list")
    public Result<PageResult<Article>> getArticleList(ArticleQueryRequest request,
                                                       Authentication authentication) {
        Long userId = authentication != null ? getCurrentUserId(authentication) : null;
        return Result.success(articleService.getArticleList(request, userId));
    }
    
    @Operation(summary = "获取文章详情")
    @GetMapping("/detail/{id}")
    public Result<Article> getArticleDetail(@PathVariable Long id, Authentication authentication) {
        Long userId = authentication != null ? getCurrentUserId(authentication) : null;
        return Result.success(articleService.getArticleDetail(id, userId));
    }
    
    @Operation(summary = "更新文章")
    @PutMapping("/update/{id}")
    public Result<Boolean> updateArticle(@PathVariable Long id,
                                         @Valid @RequestBody ArticleCreateRequest request,
                                         Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(articleService.updateArticle(id, request, userId));
    }
    
    @Operation(summary = "删除文章")
    @DeleteMapping("/delete/{id}")
    public Result<Boolean> deleteArticle(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(articleService.deleteArticle(id, userId));
    }
    
    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/like/{id}")
    public Result<Boolean> toggleLike(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(articleLikeService.toggleLike(id, userId));
    }
    
    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/favorite/{id}")
    public Result<Boolean> toggleFavorite(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Result.success(articleFavoriteService.toggleFavorite(id, userId));
    }
    
    @Operation(summary = "置顶文章")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/pin/{id}")
    public Result<Boolean> togglePinned(@PathVariable Long id, @RequestParam Integer isPinned) {
        return Result.success(articleService.togglePinned(id, isPinned));
    }
    
    @Operation(summary = "审核文章")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/approve/{id}")
    public Result<Boolean> approveArticle(@PathVariable Long id, @RequestParam Integer isApproved) {
        return Result.success(articleService.approveArticle(id, isApproved));
    }

    @Operation(summary = "获取公开统计数据")
    @GetMapping("/public/stats")
    public Result<java.util.Map<String, Object>> getPublicStats() {
        return Result.success(articleService.getPublicStats());
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        org.springframework.security.core.userdetails.User user = 
            (org.springframework.security.core.userdetails.User) userService.loadUserByUsername(username);
        com.campus.news.entity.User userEntity = userService.getUserInfo(
            userService.getOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.campus.news.entity.User>()
                .eq("username", username)).getId());
        return userEntity.getId();
    }
}
