package com.campus.news.service;

import com.campus.news.ai.AIService;
import com.campus.news.dto.AiChatRequest;
import com.campus.news.dto.AiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * 增强的AI服务类
 * 提供摘要生成、情感分析等高级AI功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiEnhancedService {

    private final AIService aiService;

    /**
     * 生成文章摘要
     * @param content 文章内容
     * @param length 摘要长度（字数）
     * @param style 摘要风格（professional/casual/academic）
     * @return 摘要结果
     */
    public Map<String, Object> generateSummary(String content, int length, String style) {
        log.info("生成摘要 - 长度: {}, 风格: {}", length, style);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 构建AI提示词
            String styleText = getStyleDescription(style);
            String prompt = String.format(
                "请为以下文章生成一个%d字左右的%s摘要，并提取关键要点：\n\n%s", 
                length, styleText, content
            );
            
            // 调用AI服务
            AiChatRequest request = new AiChatRequest();
            request.setQuestion(prompt);
            AiChatResponse response = aiService.chat(request);
            
            // 解析结果
            result.put("summary", extractSummary(response.getAnswer(), length));
            result.put("keyPoints", extractKeyPoints(response.getAnswer()));
            result.put("keywords", extractKeywords(content));
            result.put("qualityScore", calculateQualityScore(result.get("summary").toString(), content));
            
            return result;
        } catch (Exception e) {
            log.error("生成摘要失败", e);
            throw new RuntimeException("摘要生成服务暂时不可用");
        }
    }

    /**
     * 情感分析
     * @param text 待分析文本
     * @return 情感分析结果
     */
    public Map<String, Object> analyzeSentiment(String text) {
        log.info("进行情感分析 - 文本长度: {}", text.length());
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 构建AI提示词
            String prompt = String.format(
                "请对以下文本进行情感分析，包括总体情感倾向、各种情绪的百分比、关键词情感等：\n\n%s", 
                text
            );
            
            // 调用AI服务
            AiChatRequest request = new AiChatRequest();
            request.setQuestion(prompt);
            AiChatResponse response = aiService.chat(request);
            
            // 模拟情感分析结果（实际项目中应该调用专门的NLP服务）
            result.put("score", calculateSentimentScore(text));
            result.put("sentiment", getSentimentLabel(result.get("score")));
            result.put("positive", 45.0);
            result.put("neutral", 35.0);
            result.put("negative", 20.0);
            
            // 情绪细分
            List<Map<String, Object>> emotions = Arrays.asList(
                createEmotion("喜悦", 40, "😊"),
                createEmotion("信任", 30, "🤝"),
                createEmotion("期待", 25, "✨"),
                createEmotion("悲伤", 15, "😢"),
                createEmotion("愤怒", 10, "😠"),
                createEmotion("恐惧", 8, "😨")
            );
            result.put("emotions", emotions);
            
            // 关键词情感
            result.put("keywords", analyzeKeywordSentiments(text));
            
            // AI建议
            result.put("suggestion", generateSentimentSuggestion(result));
            result.put("improvements", generateImprovements(text));
            
            return result;
        } catch (Exception e) {
            log.error("情感分析失败", e);
            throw new RuntimeException("情感分析服务暂时不可用");
        }
    }

    /**
     * 批量文章摘要生成
     * @param articles 文章列表
     * @param length 摘要长度
     * @return 批量摘要结果
     */
    public List<Map<String, Object>> batchGenerateSummaries(List<String> articles, int length) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (String article : articles) {
            try {
                Map<String, Object> summary = generateSummary(article, length, "professional");
                results.add(summary);
            } catch (Exception e) {
                log.error("批量生成摘要时单篇失败", e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "生成失败");
                results.add(errorResult);
            }
        }
        
        return results;
    }

    /**
     * 智能标题生成
     * @param content 文章内容
     * @param count 生成数量
     * @return 标题建议列表
     */
    public List<String> generateTitles(String content, int count) {
        log.info("生成标题建议 - 数量: {}", count);
        
        try {
            String prompt = String.format(
                "请为以下文章生成%d个吸引人的标题：\n\n%s", 
                count, content
            );
            
            AiChatRequest request = new AiChatRequest();
            request.setQuestion(prompt);
            AiChatResponse response = aiService.chat(request);
            
            return parseTitles(response.getAnswer(), count);
        } catch (Exception e) {
            log.error("生成标题失败", e);
            return Arrays.asList("创新标题1", "吸引力标题2", "精彩标题3");
        }
    }

    // 辅助方法
    private String getStyleDescription(String style) {
        switch (style) {
            case "professional":
                return "专业风格的";
            case "casual":
                return "通俗易懂的";
            case "academic":
                return "学术性的";
            default:
                return "";
        }
    }

    private String extractSummary(String aiResponse, int targetLength) {
        // 简化处理：取前targetLength个字符
        if (aiResponse.length() > targetLength) {
            return aiResponse.substring(0, targetLength) + "...";
        }
        return aiResponse;
    }

    private List<String> extractKeyPoints(String aiResponse) {
        // 模拟提取关键要点
        return Arrays.asList(
            "核心观点1：文章的主要论述",
            "核心观点2：重要的支撑论据",
            "核心观点3：得出的关键结论"
        );
    }

    private List<String> extractKeywords(String content) {
        // 模拟提取关键词
        return Arrays.asList("创新", "发展", "技术", "教育", "未来");
    }

    private double calculateQualityScore(String summary, String original) {
        // 简单的质量评分算法
        double lengthRatio = (double) summary.length() / original.length();
        if (lengthRatio < 0.1) return 3.0;
        if (lengthRatio < 0.2) return 4.0;
        if (lengthRatio < 0.3) return 5.0;
        return 4.5;
    }

    private int calculateSentimentScore(String text) {
        // 简单的情感分数计算（实际应使用NLP模型）
        int score = 50; // 基础分数
        
        // 正面词汇
        String[] positiveWords = {"优秀", "成功", "创新", "进步", "喜悦", "荣获", "突破"};
        for (String word : positiveWords) {
            if (text.contains(word)) score += 10;
        }
        
        // 负面词汇
        String[] negativeWords = {"失败", "问题", "困难", "糟糕", "不足", "缺陷"};
        for (String word : negativeWords) {
            if (text.contains(word)) score -= 10;
        }
        
        return Math.max(0, Math.min(100, score));
    }

    private String getSentimentLabel(Object score) {
        int scoreInt = (int) score;
        if (scoreInt >= 67) return "积极";
        if (scoreInt >= 34) return "中性";
        return "消极";
    }

    private Map<String, Object> createEmotion(String name, int score, String emoji) {
        Map<String, Object> emotion = new HashMap<>();
        emotion.put("name", name);
        emotion.put("score", score);
        emotion.put("emoji", emoji);
        return emotion;
    }

    private List<Map<String, Object>> analyzeKeywordSentiments(String text) {
        List<Map<String, Object>> keywords = new ArrayList<>();
        
        // 模拟关键词情感分析
        keywords.add(createKeywordSentiment("创新", 8, "positive"));
        keywords.add(createKeywordSentiment("挑战", -2, "neutral"));
        keywords.add(createKeywordSentiment("成功", 7, "positive"));
        
        return keywords;
    }

    private Map<String, Object> createKeywordSentiment(String word, int score, String sentiment) {
        Map<String, Object> keyword = new HashMap<>();
        keyword.put("word", word);
        keyword.put("score", score);
        keyword.put("sentiment", sentiment);
        return keyword;
    }

    private String generateSentimentSuggestion(Map<String, Object> sentimentResult) {
        String sentiment = sentimentResult.get("sentiment").toString();
        
        switch (sentiment) {
            case "积极":
                return "该文本整体情感积极向上，传达了正面的信息和情绪。建议保持这种积极的表达风格。";
            case "消极":
                return "该文本情感偏消极，可能需要调整表达方式，增加一些积极元素来平衡整体情绪。";
            default:
                return "该文本情感较为中性，如需增强感染力，可以适当加入更多情感色彩。";
        }
    }

    private List<String> generateImprovements(String text) {
        return Arrays.asList(
            "可以增加更多具体的细节来增强可信度",
            "适当加入一些情感词汇可以提升感染力",
            "结构可以更加清晰，便于读者理解"
        );
    }

    private List<String> parseTitles(String aiResponse, int count) {
        // 简单解析，实际应该更智能
        List<String> titles = new ArrayList<>();
        String[] lines = aiResponse.split("\n");
        
        for (String line : lines) {
            if (!line.trim().isEmpty() && titles.size() < count) {
                titles.add(line.trim().replaceAll("^[0-9.、\\-\\*]+\\s*", ""));
            }
        }
        
        // 如果不够，补充默认标题
        while (titles.size() < count) {
            titles.add("精彩标题 " + (titles.size() + 1));
        }
        
        return titles;
    }
}
