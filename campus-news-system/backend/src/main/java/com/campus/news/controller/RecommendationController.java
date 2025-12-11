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
 * 智能推荐接口
 */
@Tag(name = "智能推荐", description = "基于AI的个性化文章推荐")
@RestController
@RequestMapping("/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "获取个性化推荐", description = "根据用户行为获取个性化文章推荐")
    @GetMapping("/personalized")
    public Result<List<Article>> getPersonalizedRecommendations(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") int count,
            @Parameter(description = "排除的文章ID") @RequestParam(required = false) List<Long> excludeIds
    ) {
        Long userId = null;
        if (token != null && token.startsWith("Bearer ")) {
            try {
                userId = jwtUtil.getUserIdFromToken(token.substring(7));
            } catch (Exception ignored) {}
        }

        List<Article> recommendations = recommendationService.getRecommendations(userId, count, excludeIds);
        return Result.success(recommendations);
    }

    @Operation(summary = "获取相似文章", description = "获取与指定文章相似的文章推荐")
    @GetMapping("/similar/{articleId}")
    public Result<List<Article>> getSimilarArticles(
            @Parameter(description = "文章ID") @PathVariable Long articleId,
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "6") int count
    ) {
        List<Article> similar = recommendationService.getSimilarArticles(articleId, count);
        return Result.success(similar);
    }

    @Operation(summary = "获取热门推荐", description = "获取热门文章推荐(无需登录)")
    @GetMapping("/hot")
    public Result<List<Article>> getHotRecommendations(
            @Parameter(description = "推荐数量") @RequestParam(defaultValue = "10") int count
    ) {
        List<Article> hot = recommendationService.getHotRecommendations(count);
        return Result.success(hot);
    }

    @Operation(summary = "推荐服务状态", description = "检查推荐服务健康状态")
    @GetMapping("/health")
    public Result<Map<String, Object>> checkHealth() {
        boolean healthy = recommendationService.isServiceHealthy();
        return Result.success(Map.of(
            "status", healthy ? "healthy" : "unavailable",
            "fallback", !healthy
        ));
    }

    @Operation(summary = "重新训练模型", description = "触发推荐模型重新训练(管理员)")
    @PostMapping("/retrain")
    public Result<String> retrainModel() {
        boolean success = recommendationService.retrainModel();
        if (success) {
            return Result.success("模型训练已触发");
        }
        return Result.error("模型训练失败");
    }
}
