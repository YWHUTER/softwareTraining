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

    @Operation(summary = "检查算法服务状态")
    @GetMapping("/health")
    public Result<Map<String, Object>> checkHealth() {
        Map<String, Object> status = new HashMap<>();
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
}
