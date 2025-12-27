package com.campus.news.controller;

import com.campus.news.common.Result;
import com.campus.news.entity.Article;
import com.campus.news.entity.Video;
import com.campus.news.service.ArticleService;
import com.campus.news.service.VideoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 分析服务控制器 - 热词分析
 */
@Tag(name = "分析服务接口")
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final ArticleService articleService;
    private final VideoService videoService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${algorithm.service.url:http://localhost:5000}")
    private String algorithmServiceUrl;

    // 缓存热词结果
    private List<Map<String, Object>> cachedHotWords = new ArrayList<>();
    private long lastCacheTime = 0;
    private static final long CACHE_DURATION = 30 * 60 * 1000; // 30分钟缓存

    @Operation(summary = "获取热门关键词")
    @GetMapping("/hotwords")
    public Result<List<Map<String, Object>>> getHotWords(
            @RequestParam(defaultValue = "30") Integer top_n,
            @RequestParam(defaultValue = "7") Integer days
    ) {
        // 先尝试Python算法服务
        try {
            String url = algorithmServiceUrl + "/api/hotwords?top_n=" + top_n + "&days=" + days;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    List<Map<String, Object>> words = new ArrayList<>();
                    for (JsonNode item : root.get("data")) {
                        Map<String, Object> word = new HashMap<>();
                        word.put("word", item.get("word").asText());
                        word.put("weight", item.get("weight").asInt());
                        word.put("count", item.get("count").asInt());
                        word.put("trend", item.get("trend").asText());
                        word.put("category", item.get("category").asText());
                        words.add(word);
                    }
                    if (!words.isEmpty()) {
                        return Result.success(words);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Python算法服务不可用，使用本地提取: {}", e.getMessage());
        }
        
        // 降级：本地提取热词
        List<Map<String, Object>> localWords = extractLocalHotWords(top_n);
        return Result.success(localWords);
    }

    /**
     * 本地提取热词（从文章和视频标题）
     */
    private List<Map<String, Object>> extractLocalHotWords(int topN) {
        // 检查缓存
        if (!cachedHotWords.isEmpty() && System.currentTimeMillis() - lastCacheTime < CACHE_DURATION) {
            return cachedHotWords.subList(0, Math.min(topN, cachedHotWords.size()));
        }

        Map<String, Integer> wordCount = new HashMap<>();
        
        try {
            // 从文章标题提取
            List<Article> articles = articleService.list();
            for (Article article : articles) {
                if (article.getTitle() != null) {
                    extractWords(article.getTitle(), wordCount, 3);
                }
            }
            
            // 从视频标题提取
            List<Video> videos = videoService.list();
            for (Video video : videos) {
                if (video.getTitle() != null) {
                    extractWords(video.getTitle(), wordCount, 2);
                }
            }
        } catch (Exception e) {
            log.warn("提取热词失败: {}", e.getMessage());
        }

        if (wordCount.isEmpty()) {
            return Collections.emptyList();
        }

        // 排序并构建结果
        int maxCount = wordCount.values().stream().max(Integer::compareTo).orElse(1);
        List<Map<String, Object>> result = wordCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(topN * 2)
                .map(entry -> {
                    Map<String, Object> word = new HashMap<>();
                    word.put("word", entry.getKey());
                    word.put("weight", (int) ((entry.getValue() * 100.0) / maxCount));
                    word.put("count", entry.getValue());
                    word.put("trend", entry.getValue() > maxCount / 2 ? "up" : "stable");
                    word.put("category", "综合");
                    return word;
                })
                .limit(topN)
                .collect(Collectors.toList());

        // 更新缓存
        cachedHotWords = result;
        lastCacheTime = System.currentTimeMillis();
        
        return result;
    }

    /**
     * 简单分词提取（提取2-4字的中文词组）
     */
    private void extractWords(String text, Map<String, Integer> wordCount, int weight) {
        if (text == null || text.isEmpty()) return;
        
        // 移除HTML标签
        text = text.replaceAll("<[^>]+>", "");
        
        // 提取中文词组（2-4个字）
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}");
        Matcher matcher = pattern.matcher(text);
        
        Set<String> stopWords = Set.of(
            "我们", "他们", "这个", "那个", "什么", "怎么", "可以", "没有", "已经",
            "进行", "开始", "结束", "通过", "根据", "关于", "对于", "由于", "为了",
            "学校", "学生", "老师", "同学", "大学", "学院", "专业", "课程",
            "工作", "生活", "学习", "发展", "建设", "服务", "管理", "活动",
            "时间", "地方", "问题", "情况", "方式", "方法", "内容", "部分"
        );
        
        while (matcher.find()) {
            String word = matcher.group();
            if (!stopWords.contains(word)) {
                wordCount.merge(word, weight, Integer::sum);
            }
        }
    }

    @Operation(summary = "获取上升趋势热词")
    @GetMapping("/hotwords/trending")
    public Result<List<Map<String, Object>>> getTrendingWords(
            @RequestParam(defaultValue = "10") Integer top_n
    ) {
        List<Map<String, Object>> allWords = getHotWords(top_n * 3, 7).getData();
        List<Map<String, Object>> trending = allWords.stream()
                .filter(w -> "up".equals(w.get("trend")))
                .limit(top_n)
                .collect(Collectors.toList());
        return Result.success(trending);
    }

    @Operation(summary = "检测新兴话题")
    @GetMapping("/hotwords/emerging")
    public Result<List<Map<String, Object>>> getEmergingTopics(
            @RequestParam(defaultValue = "3") Integer window_days,
            @RequestParam(defaultValue = "1.5") Double threshold
    ) {
        try {
            String url = algorithmServiceUrl + "/api/hotwords/emerging?window_days=" + window_days + "&threshold=" + threshold;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    List<Map<String, Object>> topics = new ArrayList<>();
                    for (JsonNode item : root.get("data")) {
                        Map<String, Object> topic = new HashMap<>();
                        topic.put("word", item.get("word").asText());
                        topic.put("current_weight", item.get("current_weight").asInt());
                        topic.put("growth_rate", item.get("growth_rate").asDouble());
                        topic.put("confidence", item.get("confidence").asDouble());
                        topic.put("category", item.has("category") ? item.get("category").asText() : "综合");
                        topics.add(topic);
                    }
                    return Result.success(topics);
                }
            }
        } catch (Exception e) {
            log.debug("获取新兴话题失败: {}", e.getMessage());
        }
        
        // 降级：从热词中筛选上升趋势的
        List<Map<String, Object>> allWords = getHotWords(30, 7).getData();
        List<Map<String, Object>> emerging = allWords.stream()
                .filter(w -> "up".equals(w.get("trend")))
                .map(w -> {
                    Map<String, Object> topic = new HashMap<>(w);
                    topic.put("current_weight", w.get("weight"));
                    topic.put("growth_rate", 1.5 + Math.random());
                    topic.put("confidence", 0.6 + Math.random() * 0.3);
                    return topic;
                })
                .limit(10)
                .collect(Collectors.toList());
        return Result.success(emerging);
    }

    @Operation(summary = "获取热词情感分布")
    @GetMapping("/hotwords/sentiment")
    public Result<Map<String, Object>> getSentimentDistribution(
            @RequestParam(defaultValue = "50") Integer top_n
    ) {
        try {
            String url = algorithmServiceUrl + "/api/hotwords/sentiment?top_n=" + top_n;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    JsonNode data = root.get("data");
                    Map<String, Object> result = new HashMap<>();
                    
                    if (data.has("distribution")) {
                        JsonNode dist = data.get("distribution");
                        Map<String, Object> distribution = new HashMap<>();
                        distribution.put("positive_count", dist.get("positive_count").asInt());
                        distribution.put("negative_count", dist.get("negative_count").asInt());
                        distribution.put("neutral_count", dist.get("neutral_count").asInt());
                        distribution.put("positive_ratio", dist.get("positive_ratio").asDouble());
                        distribution.put("negative_ratio", dist.get("negative_ratio").asDouble());
                        distribution.put("neutral_ratio", dist.get("neutral_ratio").asDouble());
                        result.put("distribution", distribution);
                    }
                    
                    result.put("total_analyzed", data.has("total_analyzed") ? data.get("total_analyzed").asInt() : top_n);
                    return Result.success(result);
                }
            }
        } catch (Exception e) {
            log.debug("获取情感分布失败: {}", e.getMessage());
        }
        
        // 降级：返回模拟的情感分布
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> distribution = new HashMap<>();
        distribution.put("positive_count", 15);
        distribution.put("negative_count", 5);
        distribution.put("neutral_count", 30);
        distribution.put("positive_ratio", 30.0);
        distribution.put("negative_ratio", 10.0);
        distribution.put("neutral_ratio", 60.0);
        result.put("distribution", distribution);
        result.put("total_analyzed", 50);
        return Result.success(result);
    }

    @Operation(summary = "检查算法服务状态")
    @GetMapping("/health")
    public Result<Map<String, Object>> checkHealth() {
        Map<String, Object> status = new HashMap<>();
        status.put("controller", "AnalysisController is working");
        status.put("timestamp", System.currentTimeMillis());
        
        try {
            String url = algorithmServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            status.put("algorithm_service", response.getStatusCode().is2xxSuccessful() ? "healthy" : "unhealthy");
            status.put("url", algorithmServiceUrl);
        } catch (Exception e) {
            status.put("algorithm_service", "unavailable");
            status.put("error", e.getMessage());
        }
        status.put("local_fallback", "available");
        return Result.success(status);
    }

    // ==================== 用户聚类分析 ====================

    @Operation(summary = "获取用户类型")
    @GetMapping("/user/type/{userId}")
    public Result<Map<String, Object>> getUserType(@PathVariable Long userId) {
        try {
            String url = algorithmServiceUrl + "/api/user/clustering/type/" + userId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    JsonNode data = root.get("data");
                    Map<String, Object> result = new HashMap<>();
                    result.put("type", data.get("type").asInt());
                    result.put("name", data.get("name").asText());
                    result.put("icon", data.get("icon").asText());
                    result.put("color", data.get("color").asText());
                    result.put("desc", data.get("desc").asText());
                    return Result.success(result);
                }
            }
        } catch (Exception e) {
            log.warn("获取用户类型失败: {}", e.getMessage());
        }
        
        // 降级返回默认类型
        Map<String, Object> defaultType = new HashMap<>();
        defaultType.put("type", 4);
        defaultType.put("name", "潜水用户");
        defaultType.put("icon", "👀");
        defaultType.put("color", "#6b7280");
        defaultType.put("desc", "浏览为主，较少互动");
        return Result.success(defaultType);
    }

    @Operation(summary = "获取用户类型分布")
    @GetMapping("/user/distribution")
    public Result<Map<String, Object>> getUserDistribution() {
        try {
            String url = algorithmServiceUrl + "/api/user/clustering/distribution";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    JsonNode data = root.get("data");
                    Map<String, Object> result = new HashMap<>();
                    result.put("total", data.get("total").asInt());
                    
                    List<Map<String, Object>> distribution = new ArrayList<>();
                    for (JsonNode item : data.get("distribution")) {
                        Map<String, Object> dist = new HashMap<>();
                        dist.put("type", item.get("type").asInt());
                        dist.put("name", item.get("name").asText());
                        dist.put("icon", item.get("icon").asText());
                        dist.put("color", item.get("color").asText());
                        dist.put("count", item.get("count").asInt());
                        dist.put("percentage", item.get("percentage").asDouble());
                        distribution.add(dist);
                    }
                    result.put("distribution", distribution);
                    return Result.success(result);
                }
            }
        } catch (Exception e) {
            log.warn("获取用户分布失败，使用本地模拟数据: {}", e.getMessage());
        }
        
        // 降级返回模拟分布数据（基于典型用户分布）
        Map<String, Object> localData = new HashMap<>();
        localData.put("total", 100);
        List<Map<String, Object>> distribution = new ArrayList<>();
        
        // 模拟用户类型分布
        distribution.add(createDistItem(0, "活跃创作者", "✍️", "#f59e0b", 15, 15.0));
        distribution.add(createDistItem(1, "深度阅读者", "📚", "#3b82f6", 25, 25.0));
        distribution.add(createDistItem(2, "社交达人", "💬", "#ec4899", 20, 20.0));
        distribution.add(createDistItem(3, "视频爱好者", "🎬", "#8b5cf6", 18, 18.0));
        distribution.add(createDistItem(4, "潜水用户", "👀", "#6b7280", 22, 22.0));
        
        localData.put("distribution", distribution);
        return Result.success(localData);
    }
    
    private Map<String, Object> createDistItem(int type, String name, String icon, String color, int count, double percentage) {
        Map<String, Object> item = new HashMap<>();
        item.put("type", type);
        item.put("name", name);
        item.put("icon", icon);
        item.put("color", color);
        item.put("count", count);
        item.put("percentage", percentage);
        return item;
    }

    // ==================== 热度预测 ====================

    @Operation(summary = "预测文章热度")
    @GetMapping("/predict/article/{articleId}")
    public Result<Map<String, Object>> predictArticleTrend(@PathVariable Long articleId) {
        try {
            String url = algorithmServiceUrl + "/api/predict/article/" + articleId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    JsonNode data = root.get("data");
                    Map<String, Object> result = new HashMap<>();
                    result.put("article_id", data.get("article_id").asLong());
                    result.put("current_views", data.get("current_views").asInt());
                    result.put("predicted_views_7d", data.get("predicted_views_7d").asInt());
                    result.put("predicted_views_30d", data.get("predicted_views_30d").asInt());
                    result.put("growth_rate", data.get("growth_rate").asDouble());
                    result.put("trend", data.get("trend").asText());
                    result.put("trend_label", data.get("trend_label").asText());
                    result.put("trend_color", data.get("trend_color").asText());
                    return Result.success(result);
                }
            }
        } catch (Exception e) {
            log.warn("预测文章热度失败: {}", e.getMessage());
        }
        
        return Result.success(Collections.emptyMap());
    }

    @Operation(summary = "预测视频热度")
    @GetMapping("/predict/video/{videoId}")
    public Result<Map<String, Object>> predictVideoTrend(@PathVariable Long videoId) {
        try {
            String url = algorithmServiceUrl + "/api/predict/video/" + videoId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    JsonNode data = root.get("data");
                    Map<String, Object> result = new HashMap<>();
                    result.put("video_id", data.get("video_id").asLong());
                    result.put("current_views", data.get("current_views").asInt());
                    result.put("predicted_views_7d", data.get("predicted_views_7d").asInt());
                    result.put("predicted_views_30d", data.get("predicted_views_30d").asInt());
                    result.put("growth_rate", data.get("growth_rate").asDouble());
                    result.put("trend", data.get("trend").asText());
                    result.put("trend_label", data.get("trend_label").asText());
                    result.put("trend_color", data.get("trend_color").asText());
                    return Result.success(result);
                }
            }
        } catch (Exception e) {
            log.warn("预测视频热度失败: {}", e.getMessage());
        }
        
        return Result.success(Collections.emptyMap());
    }

    @Operation(summary = "获取热度上升内容")
    @GetMapping("/predict/trending")
    public Result<List<Map<String, Object>>> getTrendingContent(
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "10") Integer top_n
    ) {
        try {
            String url = algorithmServiceUrl + "/api/predict/trending?content_type=" + type + "&top_n=" + top_n;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (JsonNode item : root.get("data")) {
                        Map<String, Object> content = new HashMap<>();
                        content.put("type", item.get("type").asText());
                        content.put("id", item.get("id").asLong());
                        content.put("title", item.get("title").asText());
                        content.put("current_views", item.get("current_views").asInt());
                        content.put("predicted_views_7d", item.get("predicted_views_7d").asInt());
                        content.put("growth_rate", item.get("growth_rate").asDouble());
                        content.put("trend", item.get("trend").asText());
                        content.put("trend_color", item.get("trend_color").asText());
                        results.add(content);
                    }
                    if (!results.isEmpty()) {
                        return Result.success(results);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取趋势内容失败，使用本地数据: {}", e.getMessage());
        }
        
        // 降级：从本地数据库获取热门内容
        List<Map<String, Object>> localResults = new ArrayList<>();
        
        if ("all".equals(type) || "article".equals(type)) {
            List<Article> articles = articleService.list();
            articles.stream()
                .sorted((a, b) -> Long.compare(
                    b.getViewCount() != null ? b.getViewCount() : 0,
                    a.getViewCount() != null ? a.getViewCount() : 0))
                .limit(top_n / 2)
                .forEach(article -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("type", "article");
                    item.put("id", article.getId());
                    item.put("title", article.getTitle());
                    long views = article.getViewCount() != null ? article.getViewCount() : 0;
                    item.put("current_views", views);
                    item.put("predicted_views_7d", (long)(views * 1.15));
                    item.put("growth_rate", 15.0);
                    item.put("trend", "rising");
                    item.put("trend_color", "#22c55e");
                    localResults.add(item);
                });
        }
        
        if ("all".equals(type) || "video".equals(type)) {
            List<Video> videos = videoService.list();
            videos.stream()
                .sorted((a, b) -> Long.compare(
                    b.getViewCount() != null ? b.getViewCount() : 0,
                    a.getViewCount() != null ? a.getViewCount() : 0))
                .limit(top_n / 2)
                .forEach(video -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("type", "video");
                    item.put("id", video.getId());
                    item.put("title", video.getTitle());
                    long views = video.getViewCount() != null ? video.getViewCount() : 0;
                    item.put("current_views", views);
                    item.put("predicted_views_7d", (long)(views * 1.2));
                    item.put("growth_rate", 20.0);
                    item.put("trend", "rising");
                    item.put("trend_color", "#22c55e");
                    localResults.add(item);
                });
        }
        
        return Result.success(localResults);
    }

    @Operation(summary = "获取平台趋势统计")
    @GetMapping("/predict/platform")
    public Result<Map<String, Object>> getPlatformStats() {
        try {
            String url = algorithmServiceUrl + "/api/predict/platform-stats";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("success") && root.get("success").asBoolean() && root.has("data")) {
                    JsonNode data = root.get("data");
                    Map<String, Object> result = new HashMap<>();
                    result.put("total_article_views", data.get("total_article_views").asInt());
                    result.put("total_video_views", data.get("total_video_views").asInt());
                    result.put("article_growth_rate", data.get("article_growth_rate").asDouble());
                    result.put("video_growth_rate", data.get("video_growth_rate").asDouble());
                    result.put("predicted_article_views_next_week", data.get("predicted_article_views_next_week").asInt());
                    result.put("predicted_video_views_next_week", data.get("predicted_video_views_next_week").asInt());
                    result.put("platform_trend", data.get("platform_trend").asText());
                    return Result.success(result);
                }
            }
        } catch (Exception e) {
            log.warn("获取平台统计失败: {}", e.getMessage());
        }
        
        // 降级返回本地统计
        Map<String, Object> localStats = new HashMap<>();
        long articleViews = articleService.list().stream().mapToLong(a -> a.getViewCount() != null ? a.getViewCount() : 0).sum();
        long videoViews = videoService.list().stream().mapToLong(v -> v.getViewCount() != null ? v.getViewCount() : 0).sum();
        localStats.put("total_article_views", articleViews);
        localStats.put("total_video_views", videoViews);
        localStats.put("article_growth_rate", 10.0);
        localStats.put("video_growth_rate", 15.0);
        localStats.put("predicted_article_views_next_week", (long)(articleViews * 1.1));
        localStats.put("predicted_video_views_next_week", (long)(videoViews * 1.15));
        localStats.put("platform_trend", "stable");
        return Result.success(localStats);
    }
}
