package com.campus.news.controller;

import com.campus.news.common.Result;
import com.campus.news.entity.Article;
import com.campus.news.service.RecommendationService;
import com.campus.news.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * 智能推荐控制器 (RecommendationController)
 * ============================================================================
 * 
 * 【核心功能】
 * 提供AI智能推荐的REST API接口，是前端调用推荐功能的入口
 * 
 * 【接口列表】
 * 1. GET  /recommendation/personalized     - 获取个性化文章推荐
 * 2. GET  /recommendation/similar/{id}     - 获取相似文章推荐
 * 3. GET  /recommendation/hot              - 获取热门文章推荐
 * 4. GET  /recommendation/health           - 检查推荐服务状态
 * 5. POST /recommendation/retrain          - 触发模型重训练（管理员）
 * 6. GET  /recommendation/video/personalized - 获取个性化视频推荐
 * 7. GET  /recommendation/video/hot        - 获取热门视频推荐
 * 8. GET  /recommendation/video/similar/{id} - 获取相似视频推荐
 * 
 * 【技术要点】
 * - @RestController: 标记为REST风格的控制器，返回JSON数据
 * - @RequestMapping: 设置接口的基础路径
 * - @RequiredArgsConstructor: Lombok注解，自动生成构造函数注入依赖
 * 
 * 【答辩要点】
 * Q: 前端是怎么调用推荐接口的？
 * A: 前端通过HTTP请求调用这些接口，比如 GET /api/recommendation/personalized
 * 
 * Q: 用户身份是怎么识别的？
 * A: 通过请求头中的Authorization字段携带JWT Token，后端解析Token获取用户ID
 */
@Tag(name = "智能推荐", description = "基于AI的个性化文章推荐")  // Swagger文档标签
@RestController  // 标记为REST控制器
@RequestMapping("/recommendation")  // 基础路径：/api/recommendation
@RequiredArgsConstructor  // Lombok：自动生成构造函数
public class RecommendationController {

    // ==================== 依赖注入 ====================
    
    private final RecommendationService recommendationService;  // 推荐服务
    private final JwtUtil jwtUtil;  // JWT工具类：用于解析Token获取用户ID

    // ==================== 文章推荐接口 ====================

    /**
     * 获取个性化推荐文章
     * 
     * 【接口说明】
     * GET /api/recommendation/personalized?count=10&excludeIds=1,2,3
     * 
     * 【调用流程】
     * 1. 从请求头获取JWT Token
     * 2. 解析Token获取用户ID（未登录则为null）
     * 3. 调用推荐服务获取个性化推荐
     * 4. 返回推荐文章列表
     * 
     * 【参数说明】
     * @param token      JWT令牌（可选，从请求头Authorization获取）
     * @param count      推荐数量，默认10
     * @param excludeIds 排除的文章ID列表（已看过的文章）
     * @return 推荐文章列表
     */
    @Operation(summary = "获取个性化推荐", description = "根据用户行为获取个性化文章推荐")
    @GetMapping("/personalized")
    public Result<List<Article>> getPersonalizedRecommendations(
            @RequestHeader(value = "Authorization", required = false) String token,  // 从请求头获取Token
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") int count,
            @Parameter(description = "排除的文章ID") @RequestParam(required = false) List<Long> excludeIds
    ) {
        // ========== 解析用户身份 ==========
        Long userId = null;
        if (token != null && token.startsWith("Bearer ")) {
            try {
                // 去掉"Bearer "前缀，解析Token获取用户ID
                userId = jwtUtil.getUserIdFromToken(token.substring(7));
            } catch (Exception ignored) {
                // Token无效时忽略，当作未登录用户处理
            }
        }

        // ========== 调用推荐服务 ==========
        List<Article> recommendations = recommendationService.getRecommendations(userId, count, excludeIds);
        return Result.success(recommendations);
    }

    /**
     * 获取相似文章推荐
     * 
     * 【接口说明】
     * GET /api/recommendation/similar/123?count=6
     * 
     * 【使用场景】
     * 文章详情页底部的"相关推荐"模块
     * 
     * @param articleId 当前文章ID（从URL路径获取）
     * @param count     推荐数量，默认6
     * @return 相似文章列表
     */
    @Operation(summary = "获取相似文章", description = "获取与指定文章相似的文章推荐")
    @GetMapping("/similar/{articleId}")
    public Result<List<Article>> getSimilarArticles(
            @Parameter(description = "文章ID") @PathVariable Long articleId,  // 从URL路径获取
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "6") int count
    ) {
        List<Article> similar = recommendationService.getSimilarArticles(articleId, count);
        return Result.success(similar);
    }

    /**
     * 获取热门推荐（无需登录）
     * 
     * 【接口说明】
     * GET /api/recommendation/hot?count=10
     * 
     * 【使用场景】
     * 1. 首页热门文章模块
     * 2. 未登录用户的推荐
     * 3. 新用户冷启动
     * 
     * @param count 推荐数量，默认10
     * @return 热门文章列表
     */
    @Operation(summary = "获取热门推荐", description = "获取热门文章推荐(无需登录)")
    @GetMapping("/hot")
    public Result<List<Article>> getHotRecommendations(
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") int count
    ) {
        List<Article> hot = recommendationService.getHotRecommendations(count);
        return Result.success(hot);
    }

    // ==================== 服务管理接口 ====================

    /**
     * 推荐服务状态检查
     * 
     * 【接口说明】
     * GET /api/recommendation/health
     * 
     * 【返回示例】
     * {"status": "healthy", "fallback": false}  // 服务正常
     * {"status": "unavailable", "fallback": true}  // 服务不可用，使用降级方案
     * 
     * 【使用场景】
     * 前端可以根据这个接口判断是否显示"智能推荐"标签
     */
    @Operation(summary = "推荐服务状态", description = "检查推荐服务健康状态")
    @GetMapping("/health")
    public Result<Map<String, Object>> checkHealth() {
        boolean healthy = recommendationService.isServiceHealthy();
        return Result.success(Map.of(
            "status", healthy ? "healthy" : "unavailable",
            "fallback", !healthy  // 是否使用降级方案
        ));
    }

    /**
     * 重新训练模型（管理员接口）
     * 
     * 【接口说明】
     * POST /api/recommendation/retrain
     * 
     * 【功能说明】
     * 触发Python算法服务重新训练推荐模型
     * 通常在数据更新后调用，让模型学习最新的用户行为
     * 
     * 【注意】
     * 这个接口应该加上权限控制，只允许管理员调用
     */
    @Operation(summary = "重新训练模型", description = "触发推荐模型重新训练(管理员)")
    @PostMapping("/retrain")
    public Result<String> retrainModel() {
        boolean success = recommendationService.retrainModel();
        if (success) {
            return Result.success("模型训练已触发");
        }
        return Result.error("模型训练失败");
    }

    // ==================== 视频推荐接口 ====================

    /**
     * 获取个性化视频推荐
     * 
     * 【接口说明】
     * GET /api/recommendation/video/personalized?count=10&categoryId=1
     * 
     * 【调用流程】
     * 1. 解析JWT Token获取用户ID
     * 2. 调用推荐服务，传入用户ID、数量、排除列表、分类ID
     * 3. 推荐服务调用Python算法服务获取推荐结果
     * 4. 返回推荐视频列表（包含videoId、score、reason）
     * 
     * 【返回数据格式】
     * [
     *   {"videoId": 1, "score": 0.95, "reason": "基于您的观看历史"},
     *   {"videoId": 2, "score": 0.87, "reason": "相似用户喜欢"}
     * ]
     * 
     * @param token      JWT令牌
     * @param count      推荐数量
     * @param excludeIds 排除的视频ID
     * @param categoryId 分类ID（可选，用于分类内推荐）
     * @return 推荐视频列表
     */
    @Operation(summary = "获取个性化视频推荐", description = "根据用户行为获取个性化视频推荐")
    @GetMapping("/video/personalized")
    public Result<List<Map<String, Object>>> getPersonalizedVideoRecommendations(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") int count,
            @Parameter(description = "排除的视频ID") @RequestParam(required = false) List<Long> excludeIds,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId
    ) {
        // 解析用户ID
        Long userId = null;
        if (token != null && token.startsWith("Bearer ")) {
            try {
                userId = jwtUtil.getUserIdFromToken(token.substring(7));
            } catch (Exception ignored) {}
        }

        // 调用推荐服务
        List<Map<String, Object>> recommendations = recommendationService.getVideoRecommendations(userId, count, excludeIds, categoryId);
        return Result.success(recommendations);
    }

    /**
     * 获取热门视频推荐（无需登录）
     * 
     * 【接口说明】
     * GET /api/recommendation/video/hot?count=10&categoryId=1
     * 
     * 【使用场景】
     * 1. 视频首页的热门视频模块
     * 2. 未登录用户的视频推荐
     * 3. 分类页面的热门视频
     * 
     * @param count      推荐数量
     * @param categoryId 分类ID（可选）
     * @return 热门视频列表
     */
    @Operation(summary = "获取热门视频推荐", description = "获取热门视频推荐(无需登录)")
    @GetMapping("/video/hot")
    public Result<List<Map<String, Object>>> getHotVideoRecommendations(
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") int count,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId
    ) {
        List<Map<String, Object>> hot = recommendationService.getHotVideoRecommendations(count, categoryId);
        return Result.success(hot);
    }

    /**
     * 获取相似视频推荐
     * 
     * 【接口说明】
     * GET /api/recommendation/video/similar/123?count=10
     * 
     * 【使用场景】
     * 视频播放页右侧的"相关推荐"列表
     * 
     * 【算法原理】
     * 基于视频内容（标题、标签、分类）计算相似度，
     * 返回与当前视频最相似的N个视频
     * 
     * @param videoId 当前视频ID
     * @param count   推荐数量
     * @return 相似视频列表
     */
    @Operation(summary = "获取相似视频", description = "获取与指定视频相似的视频推荐")
    @GetMapping("/video/similar/{videoId}")
    public Result<List<Map<String, Object>>> getSimilarVideos(
            @Parameter(description = "视频ID") @PathVariable Long videoId,
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") int count
    ) {
        List<Map<String, Object>> similar = recommendationService.getSimilarVideoRecommendations(videoId, count);
        return Result.success(similar);
    }
}
