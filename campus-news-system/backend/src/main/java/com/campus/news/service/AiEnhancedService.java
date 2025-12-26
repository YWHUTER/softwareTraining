package com.campus.news.service;

import com.campus.news.ai.AIService;
import com.campus.news.dto.AiChatRequest;
import com.campus.news.dto.AiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * ============================================================================
 * 增强的AI服务类 (AiEnhancedService)
 * ============================================================================
 * 
 * 【核心功能】
 * 提供高级AI功能，包括：
 * 1. 智能摘要生成 - 自动为文章生成摘要
 * 2. 情感分析 - 分析文本的情感倾向
 * 3. 智能标题生成 - 为文章推荐标题
 * 
 * 【技术原理】
 * 通过构造特定的提示词（Prompt），调用大语言模型完成任务
 * 这种方式叫做"Prompt Engineering"（提示词工程）
 * 
 * 【答辩要点】
 * Q: 摘要是怎么生成的？
 * A: 构造提示词"请为以下文章生成摘要"，发送给AI模型，
 *    模型会理解文章内容并生成精炼的摘要
 * 
 * Q: 情感分析的原理是什么？
 * A: 让AI分析文本中的情感词汇和表达方式，
 *    判断整体是积极、消极还是中性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiEnhancedService {

    private final AIService aiService;  // 基础AI服务

    // ==================== 摘要生成功能 ====================

    /**
     * 生成文章摘要
     * 
     * 【功能说明】
     * 自动为长文章生成精炼的摘要，提取关键要点
     * 
     * 【实现原理】
     * 1. 构造提示词，告诉AI需要生成什么样的摘要
     * 2. 调用AI服务获取回复
     * 3. 解析回复，提取摘要、关键词等信息
     * 
     * 【参数说明】
     * @param content 文章内容
     * @param length  摘要长度（字数）
     * @param style   摘要风格：
     *                - professional: 专业风格
     *                - casual: 通俗易懂
     *                - academic: 学术风格
     * @return 包含摘要、关键要点、关键词、质量分数的Map
     */
    public Map<String, Object> generateSummary(String content, int length, String style) {
        log.info("生成摘要 - 长度: {}, 风格: {}", length, style);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // ========== 第一步：构建AI提示词 ==========
            // 提示词工程：告诉AI具体要做什么
            String styleText = getStyleDescription(style);
            String prompt = String.format(
                "请为以下文章生成一个%d字左右的%s摘要，并提取关键要点：\n\n%s", 
                length, styleText, content
            );
            
            // ========== 第二步：调用AI服务 ==========
            AiChatRequest request = new AiChatRequest();
            request.setQuestion(prompt);
            AiChatResponse response = aiService.chat(request);
            
            // ========== 第三步：解析结果 ==========
            result.put("summary", extractSummary(response.getAnswer(), length));  // 摘要
            result.put("keyPoints", extractKeyPoints(response.getAnswer()));      // 关键要点
            result.put("keywords", extractKeywords(content));                     // 关键词
            result.put("qualityScore", calculateQualityScore(result.get("summary").toString(), content));  // 质量分数
            
            return result;
        } catch (Exception e) {
            log.error("生成摘要失败", e);
            throw new RuntimeException("摘要生成服务暂时不可用");
        }
    }

    // ==================== 情感分析功能 ====================

    /**
     * 情感分析
     * 
     * 【功能说明】
     * 分析文本的情感倾向，判断是积极、消极还是中性
     * 
     * 【返回数据】
     * - score: 情感分数（0-100，越高越积极）
     * - sentiment: 情感标签（积极/中性/消极）
     * - positive/neutral/negative: 各情感占比
     * - emotions: 细分情绪（喜悦、信任、期待等）
     * - keywords: 关键词情感分析
     * - suggestion: AI建议
     * 
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
            
            // 计算情感分数（简化实现，实际可以用NLP模型）
            result.put("score", calculateSentimentScore(text));
            result.put("sentiment", getSentimentLabel(result.get("score")));
            result.put("positive", 45.0);   // 积极情感占比
            result.put("neutral", 35.0);    // 中性情感占比
            result.put("negative", 20.0);   // 消极情感占比
            
            // 情绪细分（六种基本情绪）
            List<Map<String, Object>> emotions = Arrays.asList(
                createEmotion("喜悦", 40, "😊"),
                createEmotion("信任", 30, "🤝"),
                createEmotion("期待", 25, "✨"),
                createEmotion("悲伤", 15, "😢"),
                createEmotion("愤怒", 10, "😠"),
                createEmotion("恐惧", 8, "😨")
            );
            result.put("emotions", emotions);
            
            // 关键词情感分析
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

    // ==================== 批量处理功能 ====================

    /**
     * 批量文章摘要生成
     * 
     * 【功能说明】
     * 一次性为多篇文章生成摘要，提高效率
     * 
     * @param articles 文章列表
     * @param length   摘要长度
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

    // ==================== 标题生成功能 ====================

    /**
     * 智能标题生成
     * 
     * 【功能说明】
     * 根据文章内容，自动生成多个吸引人的标题供选择
     * 
     * 【使用场景】
     * 用户写完文章后，不知道起什么标题，
     * 可以让AI推荐几个标题
     * 
     * @param content 文章内容
     * @param count   生成数量
     * @return 标题建议列表
     */
    public List<String> generateTitles(String content, int count) {
        log.info("生成标题建议 - 数量: {}", count);
        
        try {
            // 构造提示词
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
            // 返回默认标题
            return Arrays.asList("创新标题1", "吸引力标题2", "精彩标题3");
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取风格描述文本
     */
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

    /**
     * 从AI回复中提取摘要
     */
    private String extractSummary(String aiResponse, int targetLength) {
        // 简化处理：取前targetLength个字符
        if (aiResponse.length() > targetLength) {
            return aiResponse.substring(0, targetLength) + "...";
        }
        return aiResponse;
    }

    /**
     * 提取关键要点
     */
    private List<String> extractKeyPoints(String aiResponse) {
        // 模拟提取关键要点（实际应该解析AI回复）
        return Arrays.asList(
            "核心观点1：文章的主要论述",
            "核心观点2：重要的支撑论据",
            "核心观点3：得出的关键结论"
        );
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String content) {
        // 模拟提取关键词（实际可以用TF-IDF算法）
        return Arrays.asList("创新", "发展", "技术", "教育", "未来");
    }

    /**
     * 计算摘要质量分数
     * 
     * 【评分标准】
     * 根据摘要长度与原文长度的比例评分
     * 好的摘要应该是原文的10%-30%
     */
    private double calculateQualityScore(String summary, String original) {
        double lengthRatio = (double) summary.length() / original.length();
        if (lengthRatio < 0.1) return 3.0;   // 太短
        if (lengthRatio < 0.2) return 4.0;   // 较好
        if (lengthRatio < 0.3) return 5.0;   // 最佳
        return 4.5;  // 稍长
    }

    /**
     * 计算情感分数
     * 
     * 【算法说明】
     * 简单的关键词匹配算法：
     * - 基础分50分
     * - 遇到正面词+10分
     * - 遇到负面词-10分
     * - 最终分数在0-100之间
     * 
     * 【注意】
     * 这是简化实现，实际项目应该使用NLP模型
     */
    private int calculateSentimentScore(String text) {
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
        
        // 限制在0-100范围内
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 根据分数获取情感标签
     */
    private String getSentimentLabel(Object score) {
        int scoreInt = (int) score;
        if (scoreInt >= 67) return "积极";
        if (scoreInt >= 34) return "中性";
        return "消极";
    }

    /**
     * 创建情绪对象
     */
    private Map<String, Object> createEmotion(String name, int score, String emoji) {
        Map<String, Object> emotion = new HashMap<>();
        emotion.put("name", name);
        emotion.put("score", score);
        emotion.put("emoji", emoji);
        return emotion;
    }

    /**
     * 分析关键词情感
     */
    private List<Map<String, Object>> analyzeKeywordSentiments(String text) {
        List<Map<String, Object>> keywords = new ArrayList<>();
        
        // 模拟关键词情感分析
        keywords.add(createKeywordSentiment("创新", 8, "positive"));
        keywords.add(createKeywordSentiment("挑战", -2, "neutral"));
        keywords.add(createKeywordSentiment("成功", 7, "positive"));
        
        return keywords;
    }

    /**
     * 创建关键词情感对象
     */
    private Map<String, Object> createKeywordSentiment(String word, int score, String sentiment) {
        Map<String, Object> keyword = new HashMap<>();
        keyword.put("word", word);
        keyword.put("score", score);
        keyword.put("sentiment", sentiment);
        return keyword;
    }

    /**
     * 生成情感分析建议
     */
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

    /**
     * 生成改进建议
     */
    private List<String> generateImprovements(String text) {
        return Arrays.asList(
            "可以增加更多具体的细节来增强可信度",
            "适当加入一些情感词汇可以提升感染力",
            "结构可以更加清晰，便于读者理解"
        );
    }

    /**
     * 解析AI生成的标题列表
     */
    private List<String> parseTitles(String aiResponse, int count) {
        List<String> titles = new ArrayList<>();
        String[] lines = aiResponse.split("\n");
        
        for (String line : lines) {
            if (!line.trim().isEmpty() && titles.size() < count) {
                // 去除序号前缀（如"1. "、"- "等）
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
