package com.campus.news.service;

import com.campus.news.entity.Article;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务 - 调用Python推荐微服务
 */
@Slf4j
@Service
public class RecommendationService {

    @Value("${recommendation.service.url:http://localhost:5000}")
    private String recommendationServiceUrl;

    private final RestTemplate restTemplate;
    private final ArticleService articleService;

    public RecommendationService(ArticleService articleService) {
        this.restTemplate = new RestTemplate();
        this.articleService = articleService;
    }

    /**
     * 获取个性化推荐文章
     */
    public List<Article> getRecommendations(Long userId, int count, List<Long> excludeIds) {
        try {
            String url = recommendationServiceUrl + "/api/recommend";
            
            Map<String, Object> request = new HashMap<>();
            request.put("user_id", userId);
            request.put("top_n", count);
            request.put("exclude_ids", excludeIds != null ? excludeIds : Collections.emptyList());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<RecommendationResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, RecommendationResponse.class
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                List<Long> articleIds = response.getBody().getData().stream()
                    .map(RecommendationItem::getArticleId)
                    .collect(Collectors.toList());
                
                return getArticlesByIds(articleIds);
            }
        } catch (Exception e) {
            log.error("调用推荐服务失败: {}", e.getMessage());
        }
        
        // 降级：返回热门文章
        return articleService.getHotArticles(count);
    }

    /**
     * 获取相似文章推荐
     */
    public List<Article> getSimilarArticles(Long articleId, int count) {
        try {
            String url = recommendationServiceUrl + "/api/similar/" + articleId + "?top_n=" + count;
            
            ResponseEntity<RecommendationResponse> response = restTemplate.getForEntity(
                url, RecommendationResponse.class
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                List<Long> articleIds = response.getBody().getData().stream()
                    .map(RecommendationItem::getArticleId)
                    .collect(Collectors.toList());
                
                return getArticlesByIds(articleIds);
            }
        } catch (Exception e) {
            log.error("获取相似文章失败: {}", e.getMessage());
        }
        
        return Collections.emptyList();
    }

    /**
     * 获取热门推荐(未登录用户)
     */
    public List<Article> getHotRecommendations(int count) {
        try {
            String url = recommendationServiceUrl + "/api/recommend/hot?top_n=" + count;
            
            ResponseEntity<RecommendationResponse> response = restTemplate.getForEntity(
                url, RecommendationResponse.class
            );

            if (response.getBody() != null && response.getBody().isSuccess()) {
                List<Long> articleIds = response.getBody().getData().stream()
                    .map(RecommendationItem::getArticleId)
                    .collect(Collectors.toList());
                
                return getArticlesByIds(articleIds);
            }
        } catch (Exception e) {
            log.error("获取热门推荐失败: {}", e.getMessage());
        }
        
        return articleService.getHotArticles(count);
    }

    /**
     * 检查推荐服务健康状态
     */
    public boolean isServiceHealthy() {
        try {
            String url = recommendationServiceUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("推荐服务不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 触发模型重新训练
     */
    public boolean retrainModel() {
        try {
            String url = recommendationServiceUrl + "/api/retrain";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("触发模型训练失败: {}", e.getMessage());
            return false;
        }
    }

    private List<Article> getArticlesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 保持推荐顺序
        Map<Long, Article> articleMap = articleService.getArticlesByIds(ids).stream()
            .collect(Collectors.toMap(Article::getId, a -> a));
        
        return ids.stream()
            .map(articleMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    // 响应模型
    @Data
    public static class RecommendationResponse {
        private boolean success;
        private List<RecommendationItem> data;
        private String message;
    }

    @Data
    public static class RecommendationItem {
        @JsonProperty("article_id")
        private Long articleId;
        private Double score;
        private String reason;
    }
}
