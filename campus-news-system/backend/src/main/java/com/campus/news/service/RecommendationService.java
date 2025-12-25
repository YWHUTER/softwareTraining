package com.campus.news.service;

import com.campus.news.entity.Article;
import com.campus.news.entity.Video;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

/**
 * 推荐服务 - 调用Python算法服务，失败时降级到数据库查询
 */
@Slf4j
@Service
public class RecommendationService {

    private final ArticleService articleService;
    private final VideoService videoService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${algorithm.service.url:http://localhost:5000}")
    private String algorithmServiceUrl;

    @Value("${algorithm.service.timeout:3000}")
    private int timeout;

    public RecommendationService(ArticleService articleService, VideoService videoService) {
        this.articleService = articleService;
        this.videoService = videoService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取个性化推荐文章
     */
    public List<Article> getRecommendations(Long userId, int count, List<Long> excludeIds) {
        return articleService.getHotArticles(count);
    }

    /**
     * 获取相似文章推荐
     */
    public List<Article> getSimilarArticles(Long articleId, int count) {
        return articleService.getHotArticles(count);
    }

    /**
     * 获取热门推荐
     */
    public List<Article> getHotRecommendations(int count) {
        return articleService.getHotArticles(count);
    }

    /**
     * 检查推荐服务健康状态
     */
    public boolean isServiceHealthy() {
        try {
            String url = algorithmServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("算法服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 触发模型重新训练
     */
    public boolean retrainModel() {
        try {
            String url = algorithmServiceUrl + "/api/video/retrain";
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("触发模型训练失败: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 视频推荐方法 ====================

    /**
     * 获取个性化视频推荐 - 调用算法服务
     */
    public List<Map<String, Object>> getVideoRecommendations(Long userId, int count, List<Long> excludeIds, Long categoryId) {
        try {
            String url = algorithmServiceUrl + "/api/video/recommend";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("user_id", userId);
            requestBody.put("top_n", count);
            requestBody.put("exclude_ids", excludeIds != null ? excludeIds : List.of());
            if (categoryId != null) {
                requestBody.put("category_id", categoryId);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseVideoRecommendations(response.getBody());
            }
        } catch (Exception e) {
            log.warn("调用算法服务失败，降级到热门视频: {}", e.getMessage());
        }
        
        // 降级：返回热门视频
        return getFallbackVideoRecommendations(count, "热门推荐");
    }

    /**
     * 获取热门视频推荐 - 调用算法服务
     */
    public List<Map<String, Object>> getHotVideoRecommendations(int count, Long categoryId) {
        try {
            StringBuilder urlBuilder = new StringBuilder(algorithmServiceUrl)
                .append("/api/video/recommend/hot?top_n=").append(count);
            if (categoryId != null) {
                urlBuilder.append("&category_id=").append(categoryId);
            }

            ResponseEntity<String> response = restTemplate.getForEntity(urlBuilder.toString(), String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseVideoRecommendations(response.getBody());
            }
        } catch (Exception e) {
            log.warn("调用热门视频算法失败，降级到数据库查询: {}", e.getMessage());
        }
        
        // 降级：从数据库获取热门视频
        return getFallbackVideoRecommendations(count, "热门视频");
    }

    /**
     * 获取相似视频推荐 - 调用算法服务
     */
    public List<Map<String, Object>> getSimilarVideoRecommendations(Long videoId, int count) {
        try {
            String url = algorithmServiceUrl + "/api/video/similar/" + videoId + "?top_n=" + count;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseVideoRecommendations(response.getBody());
            }
        } catch (Exception e) {
            log.warn("调用相似视频算法失败，降级到热门视频: {}", e.getMessage());
        }
        
        // 降级：返回热门视频
        return getFallbackVideoRecommendations(count, "相关推荐");
    }

    /**
     * 解析算法服务返回的视频推荐结果
     */
    private List<Map<String, Object>> parseVideoRecommendations(String responseBody) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                JsonNode dataArray = root.get("data");
                for (JsonNode item : dataArray) {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("videoId", item.get("video_id").asLong());
                    rec.put("score", item.get("score").asDouble());
                    rec.put("reason", item.get("reason").asText());
                    recommendations.add(rec);
                }
            }
        } catch (Exception e) {
            log.error("解析推荐结果失败: {}", e.getMessage());
        }
        return recommendations;
    }

    /**
     * 降级方案：从数据库获取热门视频
     */
    private List<Map<String, Object>> getFallbackVideoRecommendations(int count, String reason) {
        List<Video> hotVideos = videoService.getHotVideos(count);
        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (Video video : hotVideos) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("videoId", video.getId());
            rec.put("score", video.getViewCount() != null ? video.getViewCount().doubleValue() : 0.0);
            rec.put("reason", reason);
            recommendations.add(rec);
        }
        return recommendations;
    }
}
