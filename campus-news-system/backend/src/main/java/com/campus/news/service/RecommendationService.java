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
 * ============================================================================
 * 推荐服务 (RecommendationService)
 * ============================================================================
 * 
 * 【核心功能】
 * 这是AI推荐系统的核心服务类，负责调用Python算法服务获取个性化推荐结果。
 * 
 * 【技术架构】
 * 采用"微服务+降级"架构：
 * 1. 主服务：通过HTTP调用Python Flask算法服务（运行在5000端口）
 * 2. 降级方案：当算法服务不可用时，自动降级到数据库热门查询
 * 
 * 【推荐算法说明】（Python端实现）
 * - 协同过滤：基于用户行为相似度推荐（你喜欢的，相似用户也喜欢）
 * - 内容推荐：基于文章/视频内容相似度推荐
 * - 混合推荐：结合以上两种算法的加权结果
 * 
 * 【答辩要点】
 * Q: 为什么要用Python做推荐算法？
 * A: Python有丰富的机器学习库（如scikit-learn、pandas），适合做数据分析和推荐算法
 * 
 * Q: 什么是降级方案？
 * A: 当推荐服务不可用时，系统不会崩溃，而是返回热门内容作为兜底
 * 
 * Q: 推荐结果是实时计算的吗？
 * A: 是的，每次请求都会调用算法服务实时计算，但也可以加缓存优化
 */
@Slf4j  // Lombok注解：自动生成日志对象log
@Service  // Spring注解：标记为服务层组件，会被自动扫描注册
public class RecommendationService {

    // ==================== 依赖注入 ====================
    
    private final ArticleService articleService;  // 文章服务：用于降级时查询热门文章
    private final VideoService videoService;      // 视频服务：用于降级时查询热门视频
    private final RestTemplate restTemplate;      // HTTP客户端：用于调用Python算法服务
    private final ObjectMapper objectMapper;      // JSON解析器：解析算法服务返回的JSON数据

    // ==================== 配置项（从application.yml读取）====================
    
    /**
     * Python算法服务的URL地址
     * 默认值：http://localhost:5000（本地开发环境）
     * 生产环境可配置为：http://algorithm-server:5000
     */
    @Value("${algorithm.service.url:http://localhost:5000}")
    private String algorithmServiceUrl;

    /**
     * 调用算法服务的超时时间（毫秒）
     * 超过这个时间没响应就会触发降级
     */
    @Value("${algorithm.service.timeout:3000}")
    private int timeout;

    /**
     * 构造函数 - Spring会自动注入依赖
     * 
     * 【依赖注入原理】
     * Spring容器启动时会创建ArticleService和VideoService的实例，
     * 然后自动传入这个构造函数，完成依赖注入
     */
    public RecommendationService(ArticleService articleService, VideoService videoService) {
        this.articleService = articleService;
        this.videoService = videoService;
        this.restTemplate = new RestTemplate();  // 创建HTTP客户端
        this.objectMapper = new ObjectMapper();  // 创建JSON解析器
    }

    // ==================== 文章推荐方法 ====================

    /**
     * 获取个性化推荐文章
     * 
     * 【功能说明】
     * 根据用户的历史行为（浏览、点赞、收藏）推荐可能感兴趣的文章
     * 
     * 【参数说明】
     * @param userId     用户ID，null表示未登录用户
     * @param count      需要推荐的文章数量
     * @param excludeIds 需要排除的文章ID列表（比如用户已经看过的）
     * @return 推荐的文章列表
     * 
     * 【当前实现】
     * 简化版本：直接返回热门文章
     * 完整版本应该调用Python算法服务获取个性化推荐
     */
    public List<Article> getRecommendations(Long userId, int count, List<Long> excludeIds) {
        // TODO: 完整实现应该调用算法服务
        // 当前简化为返回热门文章
        return articleService.getHotArticles(count);
    }

    /**
     * 获取相似文章推荐
     * 
     * 【功能说明】
     * 基于内容相似度，推荐与指定文章相似的其他文章
     * 常用于文章详情页的"相关推荐"模块
     * 
     * 【算法原理】（Python端实现）
     * 1. 提取文章的关键词和标签
     * 2. 计算与其他文章的相似度（TF-IDF + 余弦相似度）
     * 3. 返回相似度最高的N篇文章
     * 
     * @param articleId 当前文章ID
     * @param count     推荐数量
     * @return 相似文章列表
     */
    public List<Article> getSimilarArticles(Long articleId, int count) {
        // TODO: 完整实现应该调用算法服务的相似度计算接口
        return articleService.getHotArticles(count);
    }

    /**
     * 获取热门推荐（无需登录）
     * 
     * 【功能说明】
     * 返回全站热门文章，适用于：
     * 1. 未登录用户的首页推荐
     * 2. 新用户冷启动（没有历史行为数据）
     * 3. 算法服务降级时的兜底方案
     * 
     * @param count 推荐数量
     * @return 热门文章列表
     */
    public List<Article> getHotRecommendations(int count) {
        return articleService.getHotArticles(count);
    }

    // ==================== 服务健康检查 ====================

    /**
     * 检查推荐服务健康状态
     * 
     * 【功能说明】
     * 检测Python算法服务是否正常运行
     * 前端可以根据这个状态决定是否显示"智能推荐"功能
     * 
     * 【实现原理】
     * 向算法服务的/health端点发送GET请求，如果返回200则健康
     * 
     * @return true=服务正常，false=服务不可用
     */
    public boolean isServiceHealthy() {
        try {
            // 拼接健康检查URL：http://localhost:5000/health
            String url = algorithmServiceUrl + "/health";
            // 发送GET请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            // 判断HTTP状态码是否为200
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            // 任何异常都视为服务不健康（网络错误、超时、服务未启动等）
            log.warn("算法服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 触发模型重新训练
     * 
     * 【功能说明】
     * 通知Python算法服务重新训练推荐模型
     * 通常在以下场景调用：
     * 1. 管理员手动触发
     * 2. 定时任务（如每天凌晨）
     * 3. 数据量达到一定阈值
     * 
     * 【训练过程】（Python端）
     * 1. 从数据库加载最新的用户行为数据
     * 2. 重新计算用户-物品矩阵
     * 3. 更新推荐模型参数
     * 
     * @return true=训练成功触发，false=触发失败
     */
    public boolean retrainModel() {
        try {
            // 调用算法服务的重训练接口
            String url = algorithmServiceUrl + "/api/video/retrain";
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("触发模型训练失败: {}", e.getMessage());
            return false;
        }
    }

    // ==================== 视频推荐方法（核心功能）====================

    /**
     * 获取个性化视频推荐 - 调用算法服务
     * 
     * 【这是最核心的推荐方法，答辩重点！】
     * 
     * 【功能说明】
     * 根据用户的观看历史、点赞、收藏等行为，推荐个性化视频
     * 
     * 【调用流程】
     * 1. 构建请求参数（用户ID、数量、排除列表等）
     * 2. 发送HTTP POST请求到Python算法服务
     * 3. 解析返回的JSON结果
     * 4. 如果调用失败，降级到热门视频
     * 
     * 【算法服务返回格式】
     * {
     *   "success": true,
     *   "data": [
     *     {"video_id": 1, "score": 0.95, "reason": "基于您的观看历史"},
     *     {"video_id": 2, "score": 0.87, "reason": "相似用户喜欢"}
     *   ]
     * }
     * 
     * @param userId     用户ID（null表示未登录）
     * @param count      推荐数量
     * @param excludeIds 排除的视频ID（已看过的）
     * @param categoryId 分类ID（可选，用于分类内推荐）
     * @return 推荐结果列表，包含videoId、score、reason
     */
    public List<Map<String, Object>> getVideoRecommendations(Long userId, int count, List<Long> excludeIds, Long categoryId) {
        try {
            // ========== 第一步：构建请求URL ==========
            String url = algorithmServiceUrl + "/api/video/recommend";
            
            // ========== 第二步：构建请求体（JSON格式）==========
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("user_id", userId);           // 用户ID
            requestBody.put("top_n", count);              // 推荐数量
            requestBody.put("exclude_ids", excludeIds != null ? excludeIds : List.of());  // 排除列表
            if (categoryId != null) {
                requestBody.put("category_id", categoryId);  // 可选的分类过滤
            }

            // ========== 第三步：设置HTTP请求头 ==========
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);  // 告诉服务器我们发送的是JSON
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // ========== 第四步：发送POST请求 ==========
            // restTemplate.postForEntity() 会：
            // 1. 将requestBody序列化为JSON
            // 2. 发送HTTP POST请求
            // 3. 接收响应并返回
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            // ========== 第五步：解析响应 ==========
            if (response.getStatusCode() == HttpStatus.OK) {
                // 调用成功，解析JSON结果
                return parseVideoRecommendations(response.getBody());
            }
        } catch (Exception e) {
            // ========== 异常处理：降级方案 ==========
            // 可能的异常：网络超时、服务未启动、JSON解析错误等
            log.warn("调用算法服务失败，降级到热门视频: {}", e.getMessage());
        }
        
        // 降级：返回热门视频作为兜底
        return getFallbackVideoRecommendations(count, "热门推荐");
    }

    /**
     * 获取热门视频推荐 - 调用算法服务
     * 
     * 【功能说明】
     * 获取全站热门视频，不需要用户登录
     * 热门度计算公式（Python端）：
     * 热门分 = 播放量 * 0.4 + 点赞数 * 0.3 + 评论数 * 0.2 + 收藏数 * 0.1
     * 
     * @param count      推荐数量
     * @param categoryId 分类ID（可选）
     * @return 热门视频列表
     */
    public List<Map<String, Object>> getHotVideoRecommendations(int count, Long categoryId) {
        try {
            // 构建GET请求URL，带查询参数
            StringBuilder urlBuilder = new StringBuilder(algorithmServiceUrl)
                .append("/api/video/recommend/hot?top_n=").append(count);
            if (categoryId != null) {
                urlBuilder.append("&category_id=").append(categoryId);
            }

            // 发送GET请求
            ResponseEntity<String> response = restTemplate.getForEntity(urlBuilder.toString(), String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseVideoRecommendations(response.getBody());
            }
        } catch (Exception e) {
            log.warn("调用热门视频算法失败，降级到数据库查询: {}", e.getMessage());
        }
        
        // 降级方案
        return getFallbackVideoRecommendations(count, "热门视频");
    }

    /**
     * 获取相似视频推荐 - 调用算法服务
     * 
     * 【功能说明】
     * 基于内容相似度，推荐与当前视频相似的其他视频
     * 用于视频播放页的"相关推荐"
     * 
     * 【相似度计算原理】（Python端）
     * 1. 提取视频特征：标题关键词、标签、分类、时长等
     * 2. 构建特征向量
     * 3. 计算余弦相似度
     * 4. 返回相似度最高的N个视频
     * 
     * @param videoId 当前视频ID
     * @param count   推荐数量
     * @return 相似视频列表
     */
    public List<Map<String, Object>> getSimilarVideoRecommendations(Long videoId, int count) {
        try {
            // 构建URL：/api/video/similar/123?top_n=10
            String url = algorithmServiceUrl + "/api/video/similar/" + videoId + "?top_n=" + count;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return parseVideoRecommendations(response.getBody());
            }
        } catch (Exception e) {
            log.warn("调用相似视频算法失败，降级到热门视频: {}", e.getMessage());
        }
        
        // 降级方案
        return getFallbackVideoRecommendations(count, "相关推荐");
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析算法服务返回的视频推荐结果
     * 
     * 【JSON解析说明】
     * 算法服务返回的JSON格式：
     * {
     *   "success": true,
     *   "data": [
     *     {"video_id": 1, "score": 0.95, "reason": "推荐理由"},
     *     ...
     *   ]
     * }
     * 
     * 我们需要提取data数组中的每个推荐项
     * 
     * @param responseBody 算法服务返回的JSON字符串
     * @return 解析后的推荐列表
     */
    private List<Map<String, Object>> parseVideoRecommendations(String responseBody) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        try {
            // 使用Jackson解析JSON
            JsonNode root = objectMapper.readTree(responseBody);
            
            // 检查返回是否成功，并获取data数组
            if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                JsonNode dataArray = root.get("data");
                
                // 遍历每个推荐项
                for (JsonNode item : dataArray) {
                    Map<String, Object> rec = new HashMap<>();
                    rec.put("videoId", item.get("video_id").asLong());   // 视频ID
                    rec.put("score", item.get("score").asDouble());      // 推荐分数（0-1）
                    rec.put("reason", item.get("reason").asText());      // 推荐理由
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
     * 
     * 【降级策略说明】
     * 当算法服务不可用时（网络问题、服务宕机等），
     * 系统不会报错，而是返回数据库中的热门视频作为兜底。
     * 
     * 这样保证了：
     * 1. 用户体验不受影响（总能看到推荐内容）
     * 2. 系统稳定性（不会因为算法服务挂掉而崩溃）
     * 
     * 【答辩要点】
     * Q: 什么是服务降级？
     * A: 当依赖的服务不可用时，提供一个备选方案，保证核心功能可用
     * 
     * @param count  需要的视频数量
     * @param reason 推荐理由（显示给用户看的）
     * @return 热门视频列表
     */
    private List<Map<String, Object>> getFallbackVideoRecommendations(int count, String reason) {
        // 从数据库查询热门视频（按播放量排序）
        List<Video> hotVideos = videoService.getHotVideos(count);
        
        List<Map<String, Object>> recommendations = new ArrayList<>();
        for (Video video : hotVideos) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("videoId", video.getId());
            // 用播放量作为分数（归一化处理会更好）
            rec.put("score", video.getViewCount() != null ? video.getViewCount().doubleValue() : 0.0);
            rec.put("reason", reason);  // 降级时显示通用理由
            recommendations.add(rec);
        }
        return recommendations;
    }
}
