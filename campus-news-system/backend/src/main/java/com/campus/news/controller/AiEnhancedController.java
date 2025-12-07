package com.campus.news.controller;

import com.campus.news.common.Result;
import com.campus.news.service.AiEnhancedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 增强AI功能控制器
 * 提供摘要生成、情感分析等高级功能
 */
@Slf4j
@Tag(name = "增强AI功能接口")
@RestController
@RequestMapping("/ai/enhanced")
@RequiredArgsConstructor
public class AiEnhancedController {

    private final AiEnhancedService aiEnhancedService;

    /**
     * 生成文章摘要
     */
    @Operation(summary = "生成文章摘要")
    @PostMapping("/summary")
    public Result<Map<String, Object>> generateSummary(@RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        Integer length = (Integer) request.getOrDefault("length", 200);
        String style = (String) request.getOrDefault("style", "professional");
        
        log.info("生成摘要请求 - 长度: {}, 风格: {}", length, style);
        
        try {
            Map<String, Object> result = aiEnhancedService.generateSummary(content, length, style);
            return Result.success(result);
        } catch (Exception e) {
            log.error("生成摘要失败", e);
            return Result.error("摘要生成失败: " + e.getMessage());
        }
    }

    /**
     * 情感分析
     */
    @Operation(summary = "文本情感分析")
    @PostMapping("/sentiment")
    public Result<Map<String, Object>> analyzeSentiment(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");
        
        log.info("情感分析请求 - 文本长度: {}", text != null ? text.length() : 0);
        
        try {
            Map<String, Object> result = aiEnhancedService.analyzeSentiment(text);
            return Result.success(result);
        } catch (Exception e) {
            log.error("情感分析失败", e);
            return Result.error("情感分析失败: " + e.getMessage());
        }
    }

    /**
     * 批量生成摘要
     */
    @Operation(summary = "批量生成摘要")
    @PostMapping("/summary/batch")
    public Result<List<Map<String, Object>>> batchGenerateSummaries(@RequestBody Map<String, Object> request) {
        List<String> articles = (List<String>) request.get("articles");
        Integer length = (Integer) request.getOrDefault("length", 200);
        
        log.info("批量生成摘要 - 数量: {}, 长度: {}", articles.size(), length);
        
        try {
            List<Map<String, Object>> results = aiEnhancedService.batchGenerateSummaries(articles, length);
            return Result.success(results);
        } catch (Exception e) {
            log.error("批量生成摘要失败", e);
            return Result.error("批量处理失败: " + e.getMessage());
        }
    }

    /**
     * 生成标题建议
     */
    @Operation(summary = "生成标题建议")
    @PostMapping("/titles")
    public Result<List<String>> generateTitles(@RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        Integer count = (Integer) request.getOrDefault("count", 3);
        
        log.info("生成标题建议 - 数量: {}", count);
        
        try {
            List<String> titles = aiEnhancedService.generateTitles(content, count);
            return Result.success(titles);
        } catch (Exception e) {
            log.error("生成标题失败", e);
            return Result.error("标题生成失败: " + e.getMessage());
        }
    }

    /**
     * 文章质量评估
     */
    @Operation(summary = "文章质量评估")
    @PostMapping("/quality")
    public Result<Map<String, Object>> evaluateQuality(@RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        
        log.info("文章质量评估 - 文本长度: {}", content != null ? content.length() : 0);
        
        try {
            // 综合评估文章质量
            Map<String, Object> sentimentResult = aiEnhancedService.analyzeSentiment(content);
            Map<String, Object> summaryResult = aiEnhancedService.generateSummary(content, 100, "professional");
            
            // 构建质量评估结果
            Map<String, Object> qualityResult = Map.of(
                "overallScore", calculateOverallScore(sentimentResult, summaryResult),
                "readability", evaluateReadability(content),
                "emotionalImpact", sentimentResult.get("score"),
                "contentDensity", evaluateContentDensity(content),
                "suggestions", generateQualitySuggestions(content)
            );
            
            return Result.success(qualityResult);
        } catch (Exception e) {
            log.error("质量评估失败", e);
            return Result.error("质量评估失败: " + e.getMessage());
        }
    }

    // 辅助方法
    private double calculateOverallScore(Map<String, Object> sentiment, Map<String, Object> summary) {
        double sentimentScore = ((Number) sentiment.get("score")).doubleValue() / 100.0 * 5;
        double summaryScore = ((Number) summary.get("qualityScore")).doubleValue();
        return (sentimentScore + summaryScore) / 2.0;
    }

    private double evaluateReadability(String content) {
        // 简单的可读性评估
        if (content == null) return 0;
        
        int sentenceCount = content.split("[。！？]").length;
        int wordCount = content.length();
        double avgSentenceLength = (double) wordCount / sentenceCount;
        
        // 句子长度适中得分高
        if (avgSentenceLength < 20) return 3.0;
        if (avgSentenceLength < 40) return 4.5;
        if (avgSentenceLength < 60) return 4.0;
        return 3.5;
    }

    private double evaluateContentDensity(String content) {
        // 内容密度评估
        if (content == null || content.isEmpty()) return 0;
        
        // 计算有意义词汇的比例（简化版）
        String[] meaningfulWords = {"但是", "因此", "所以", "虽然", "不仅", "而且", "首先", "其次", "最后"};
        int count = 0;
        for (String word : meaningfulWords) {
            if (content.contains(word)) count++;
        }
        
        return Math.min(5.0, count * 0.5 + 3.0);
    }

    private List<String> generateQualitySuggestions(String content) {
        return List.of(
            "增加具体数据和案例支撑论点",
            "优化段落结构，使逻辑更清晰",
            "适当增加过渡词增强文章连贯性",
            "检查并修正可能的语法错误"
        );
    }
}
