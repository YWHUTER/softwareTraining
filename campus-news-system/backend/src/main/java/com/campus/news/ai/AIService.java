package com.campus.news.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.news.dto.AiChatRequest;
import com.campus.news.dto.AiChatResponse;
import com.campus.news.entity.Article;
import com.campus.news.entity.User;
import com.campus.news.entity.UserFollow;
import com.campus.news.mapper.ArticleMapper;
import com.campus.news.mapper.UserMapper;
import com.campus.news.mapper.UserFollowMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI服务接口tao gt8
 * 后期可接入大模型API实现智能功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final UserFollowMapper userFollowMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Kimi API 配置
    private static final String KIMI_API_URL = "https://api.moonshot.cn/v1/chat/completions";
    private static final String KIMI_API_KEY = "sk-4hnnoqUMCqXuGIkZ1mwAZHv2RWDFbSeP4WHQWrtosP0FwIGw";
    private static final String KIMI_MODEL = "moonshot-v1-8k";  // 可选: moonshot-v1-8k, moonshot-v1-32k, moonshot-v1-128k
    
    // DeepSeek API 配置
    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions";
    private static final String DEEPSEEK_API_KEY = "sk-13c4824da49f430ea15255cfbccf46be";
    private static final String DEEPSEEK_MODEL = "deepseek-chat";  // 可选: deepseek-chat, deepseek-coder
    
    // 豆包 API 配置（火山引擎）
    private static final String DOUBAO_API_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
    private static final String DOUBAO_API_KEY = "771ea12e-5ee3-4c36-9927-11a7584fb8e4";
    private static final String DOUBAO_MODEL = "ep-20251203124851-pc6tv";
    
    private static final String AI_CHAT_CACHE_PREFIX = "ai:chat:";
    private static final long CACHE_EXPIRE_HOURS = 24;
    
    /**
     * 处理AI聊天请求
     * @param request 聊天请求
     * @return 聊天响应
     */
    public AiChatResponse chat(AiChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        // 获取选择的模型，默认为 kimi
        String model = request.getModel();
        if (model == null || model.isEmpty()) {
            model = "kimi";
        }
        
        // 调用大模型获取回复
        String answer = callLLM(request.getQuestion(), model);
        
        // 构建响应
        AiChatResponse response = AiChatResponse.builder()
                .answer(answer)
                .sessionId(sessionId)
                .timestamp(System.currentTimeMillis())
                .build();
        
        // 缓存对话记录到 Redis（可选）
        cacheConversation(sessionId, request.getQuestion(), answer);
        
        return response;
    }
    
    /**
     * 调用大模型 API（支持 Kimi 和 DeepSeek）
     * 
     * @param prompt 用户输入的问题
     * @param modelType 模型类型：kimi 或 deepseek
     * @return AI 回复内容
     */
    public String callLLM(String prompt, String modelType) {
        log.info("收到AI请求，模型: {}, 问题: {}", modelType, prompt);
        
        // 根据模型类型选择 API 配置
        String apiUrl;
        String apiKey;
        String modelName;
        
        if ("deepseek".equalsIgnoreCase(modelType)) {
            apiUrl = DEEPSEEK_API_URL;
            apiKey = DEEPSEEK_API_KEY;
            modelName = DEEPSEEK_MODEL;
        } else if ("doubao".equalsIgnoreCase(modelType)) {
            apiUrl = DOUBAO_API_URL;
            apiKey = DOUBAO_API_KEY;
            modelName = DOUBAO_MODEL;
        } else {
            // 默认使用 Kimi
            apiUrl = KIMI_API_URL;
            apiKey = KIMI_API_KEY;
            modelName = KIMI_MODEL;
        }
        
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelName);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统提示词（设定AI角色，包含系统详细信息）
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", buildSystemPrompt(modelType));
            messages.add(systemMessage);
            
            // 🔍 检测是否需要查询数据，并获取实时数据上下文
            String dataContext = buildDataContext(prompt);
            if (!dataContext.isEmpty()) {
                Map<String, String> dataMessage = new HashMap<>();
                dataMessage.put("role", "system");
                dataMessage.put("content", """
                    ## 📊 实时数据查询结果
                    
                    以下是我刚从系统数据库中为用户查询到的最新数据。请基于这些真实数据来回答用户问题：
                    
                    """ + dataContext + """
                    
                    ## 💡 数据呈现指南
                    - 如果是文章列表，请用简洁的编号列表呈现，包含标题、浏览量等关键信息
                    - 如果是统计数据，请用清晰的格式呈现，可适当加入对比或趋势分析
                    - 涉及具体文章时，务必提供访问链接格式：/article/{文章ID}
                    - 回答要自然流畅，像是你亲自查询后告诉用户的
                    - 可以基于数据给出简要分析或推荐
                    """);
                messages.add(dataMessage);
            }
            
            // 用户消息
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            
            requestBody.put("messages", messages);
            
            // 发送请求
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            
            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String content = choices.get(0).path("message").path("content").asText();
                    log.info("{} AI 回复成功", modelType.toUpperCase());
                    return content;
                }
            }
            
            log.warn("{} API 响应异常: {}", modelType, response.getBody());
            return "抱歉，AI 服务暂时无法响应，请稍后再试。";
            
        } catch (Exception e) {
            log.error("调用 {} API 失败", modelType, e);
            // 发生错误时返回 Mock 回复
            return generateFallbackResponse(prompt);
        }
    }
    
    /**
     * 调用大模型 API（兼容旧接口，默认使用 Kimi）
     */
    public String callLLM(String prompt) {
        return callLLM(prompt, "kimi");
    }
    
    // 线程池用于异步处理流式请求
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();
    
    /**
     * 流式聊天接口 - 实现类ChatGPT的打字机效果
     * 使用 SSE (Server-Sent Events) 实时推送AI回复
     *
     * @param request 聊天请求
     * @param emitter SSE发射器
     */
    public void streamChat(AiChatRequest request, SseEmitter emitter) {
        streamExecutor.execute(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            StringBuilder fullResponse = new StringBuilder();
            
            try {
                String prompt = request.getQuestion();
                String modelType = request.getModel();
                if (modelType == null || modelType.isEmpty()) {
                    modelType = "kimi";
                }
                
                // 获取数据上下文
                String dataContext = buildDataContext(prompt);
                
                // 根据模型类型选择 API 配置
                String apiUrl;
                String apiKey;
                String modelName;
                
                if ("deepseek".equalsIgnoreCase(modelType)) {
                    apiUrl = DEEPSEEK_API_URL;
                    apiKey = DEEPSEEK_API_KEY;
                    modelName = DEEPSEEK_MODEL;
                } else if ("doubao".equalsIgnoreCase(modelType)) {
                    apiUrl = DOUBAO_API_URL;
                    apiKey = DOUBAO_API_KEY;
                    modelName = DOUBAO_MODEL;
                } else {
                    apiUrl = KIMI_API_URL;
                    apiKey = KIMI_API_KEY;
                    modelName = KIMI_MODEL;
                }
                
                // 构建消息列表
                List<Map<String, String>> messages = new ArrayList<>();
                
                // 系统提示词
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", buildSystemPrompt(modelType));
                messages.add(systemMessage);
                
                // 数据上下文
                if (!dataContext.isEmpty()) {
                    Map<String, String> dataMessage = new HashMap<>();
                    dataMessage.put("role", "system");
                    dataMessage.put("content", "## 📊 实时数据查询结果\n\n" + dataContext);
                    messages.add(dataMessage);
                }
                
                // 用户消息
                Map<String, String> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", prompt);
                messages.add(userMessage);
                
                // 构建请求体（启用流式）
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", modelName);
                requestBody.put("messages", messages);
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 2000);
                requestBody.put("stream", true);  // 🔥 关键：启用流式输出
                
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                
                // 创建连接
                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Accept", "text/event-stream");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(120000);
                
                // 发送请求
                connection.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));
                connection.getOutputStream().flush();
                
                // 读取流式响应
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String line;
                
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        
                        // 检查是否结束
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        
                        try {
                            // 解析 JSON 获取内容增量
                            JsonNode node = objectMapper.readTree(data);
                            JsonNode choices = node.path("choices");
                            if (choices.isArray() && choices.size() > 0) {
                                JsonNode delta = choices.get(0).path("delta");
                                String content = delta.path("content").asText("");
                                
                                if (!content.isEmpty()) {
                                    fullResponse.append(content);
                                    
                                    // 发送 SSE 事件
                                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                                            .data(objectMapper.writeValueAsString(Map.of(
                                                    "content", content,
                                                    "done", false
                                            )));
                                    emitter.send(event);
                                }
                            }
                        } catch (Exception e) {
                            log.debug("解析流式数据失败: {}", data);
                        }
                    }
                }
                
                // 发送完成事件
                String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(Map.of(
                        "content", "",
                        "done", true,
                        "sessionId", sessionId
                ))));
                
                // 缓存完整对话
                cacheConversation(sessionId, prompt, fullResponse.toString());
                
                emitter.complete();
                log.info("流式聊天完成，回复长度: {}", fullResponse.length());
                
            } catch (Exception e) {
                log.error("流式聊天失败", e);
                try {
                    // 发送错误信息
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(Map.of(
                            "content", "抱歉，AI服务暂时出现问题，请稍后重试。",
                            "done", true,
                            "error", true
                    ))));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                if (reader != null) {
                    try { reader.close(); } catch (Exception ignored) {}
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }
    
    /**
     * 根据用户问题构建数据上下文
     * 检测问题类型，查询相应数据
     */
    private String buildDataContext(String prompt) {
        StringBuilder context = new StringBuilder();
        String lowerPrompt = prompt.toLowerCase();
        
        try {
            // 🔥 热门/热度相关查询
            if (containsAny(lowerPrompt, "热门", "热度", "最火", "浏览量最高", "最多人看", "火爆", "受欢迎")) {
                context.append(getHotArticles());
            }
            
            // 📅 最新文章查询
            if (containsAny(lowerPrompt, "最新", "最近", "今天", "刚发布", "新发布")) {
                context.append(getLatestArticles());
            }
            
            // 📊 统计数据查询
            if (containsAny(lowerPrompt, "多少篇", "文章数", "统计", "总共", "一共", "数量")) {
                context.append(getStatistics());
            }
            
            // 🏷️ 分类查询
            if (containsAny(lowerPrompt, "官方新闻", "官方")) {
                context.append(getArticlesByType("OFFICIAL", "官方新闻"));
            }
            if (containsAny(lowerPrompt, "全校新闻", "全校")) {
                context.append(getArticlesByType("CAMPUS", "全校新闻"));
            }
            if (containsAny(lowerPrompt, "学院新闻", "学院")) {
                context.append(getArticlesByType("COLLEGE", "学院新闻"));
            }
            
            // 📌 置顶文章查询
            if (containsAny(lowerPrompt, "置顶", "推荐", "重要")) {
                context.append(getPinnedArticles());
            }
            
            // 👤 用户相关查询
            if (containsAny(lowerPrompt, "用户数", "多少用户", "注册用户")) {
                context.append(getUserStatistics());
            }
            
            // ⭐ 关注相关查询
            if (containsAny(lowerPrompt, "粉丝", "关注", "最多粉丝", "人气", "大V", "网红")) {
                context.append(getFollowStatistics());
            }
            
            // 🏆 粉丝排行
            if (containsAny(lowerPrompt, "粉丝最多", "最受欢迎", "人气最高", "粉丝排行", "谁最火")) {
                context.append(getTopFollowedUsers());
            }
            
            // 🔍 搜索功能 - 提取关键词并搜索文章
            String searchKeyword = extractSearchKeyword(prompt);
            if (searchKeyword != null && !searchKeyword.isEmpty()) {
                context.append(searchArticles(searchKeyword));
            }
            
            // 📈 今日数据查询
            if (containsAny(lowerPrompt, "今天", "今日", "当天")) {
                context.append(getTodayStatistics());
            }
            
            // 💬 评论相关
            if (containsAny(lowerPrompt, "评论", "互动", "讨论")) {
                context.append(getCommentStatistics());
            }
            
            // 🏆 作者排行
            if (containsAny(lowerPrompt, "作者", "谁发的多", "写文章最多", "活跃用户")) {
                context.append(getTopAuthors());
            }
            
        } catch (Exception e) {
            log.warn("查询数据失败: {}", e.getMessage());
        }
        
        return context.toString();
    }
    
    /**
     * 获取今日统计数据
     */
    private String getTodayStatistics() {
        StringBuilder sb = new StringBuilder("\n【今日数据统计】\n");
        try {
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            
            // 今日新文章
            QueryWrapper<Article> articleWrapper = new QueryWrapper<>();
            articleWrapper.ge("created_at", todayStart).eq("is_approved", 1);
            long todayArticles = articleMapper.selectCount(articleWrapper);
            sb.append("- 今日新发布文章: ").append(todayArticles).append(" 篇\n");
            
            // 今日新用户
            QueryWrapper<User> userWrapper = new QueryWrapper<>();
            userWrapper.ge("created_at", todayStart);
            long todayUsers = userMapper.selectCount(userWrapper);
            sb.append("- 今日新注册用户: ").append(todayUsers).append(" 人\n");
            
        } catch (Exception e) {
            sb.append("数据查询出错\n");
        }
        return sb.toString();
    }
    
    /**
     * 获取评论统计
     */
    private String getCommentStatistics() {
        StringBuilder sb = new StringBuilder("\n【评论互动统计】\n");
        sb.append("- 系统支持文章评论、回复评论等互动功能\n");
        sb.append("- 用户可以在文章详情页发表评论\n");
        sb.append("- 评论会通知文章作者\n");
        return sb.toString();
    }
    
    /**
     * 获取活跃作者排行
     */
    private String getTopAuthors() {
        StringBuilder sb = new StringBuilder("\n【活跃作者排行】\n");
        try {
            // 查询发文最多的用户
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.eq("is_approved", 1)
                   .select("author_id", "COUNT(*) as article_count")
                   .groupBy("author_id")
                   .orderByDesc("article_count")
                   .last("LIMIT 5");
            List<Map<String, Object>> results = articleMapper.selectMaps(wrapper);
            
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> result = results.get(i);
                Long authorId = ((Number) result.get("author_id")).longValue();
                Long count = ((Number) result.get("article_count")).longValue();
                User author = userMapper.selectById(authorId);
                if (author != null) {
                    sb.append(String.format("%d. %s - 发布 %d 篇文章\n", 
                        i + 1, author.getUsername(), count));
                }
            }
        } catch (Exception e) {
            sb.append("查询出错\n");
        }
        return sb.toString();
    }
    
    /**
     * 检查字符串是否包含任意关键词
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取热门文章（按浏览量排序）
     */
    private String getHotArticles() {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("is_approved", 1)
               .orderByDesc("view_count")
               .last("LIMIT 5");
        List<Article> articles = articleMapper.selectList(wrapper);
        
        if (articles.isEmpty()) {
            return "【热门文章】：暂无数据\n\n";
        }
        
        StringBuilder sb = new StringBuilder("【热门文章 TOP5】：\n");
        for (int i = 0; i < articles.size(); i++) {
            Article a = articles.get(i);
            sb.append(String.format("%d. 《%s》 - 浏览量：%d，板块：%s\n", 
                i + 1, a.getTitle(), a.getViewCount(), getBoardTypeName(a.getBoardType())));
        }
        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * 获取最新文章
     */
    private String getLatestArticles() {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("is_approved", 1)
               .orderByDesc("created_at")
               .last("LIMIT 5");
        List<Article> articles = articleMapper.selectList(wrapper);
        
        if (articles.isEmpty()) {
            return "【最新文章】：暂无数据\n\n";
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        StringBuilder sb = new StringBuilder("【最新文章 TOP5】：\n");
        for (int i = 0; i < articles.size(); i++) {
            Article a = articles.get(i);
            String time = a.getCreatedAt() != null ? a.getCreatedAt().format(formatter) : "未知";
            sb.append(String.format("%d. 《%s》 - 发布时间：%s，板块：%s\n", 
                i + 1, a.getTitle(), time, getBoardTypeName(a.getBoardType())));
        }
        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * 获取统计数据
     */
    private String getStatistics() {
        // 总文章数
        long totalArticles = articleMapper.selectCount(new QueryWrapper<Article>().eq("is_approved", 1));
        
        // 各板块文章数
        long officialCount = articleMapper.selectCount(new QueryWrapper<Article>()
                .eq("is_approved", 1).eq("board_type", "OFFICIAL"));
        long campusCount = articleMapper.selectCount(new QueryWrapper<Article>()
                .eq("is_approved", 1).eq("board_type", "CAMPUS"));
        long collegeCount = articleMapper.selectCount(new QueryWrapper<Article>()
                .eq("is_approved", 1).eq("board_type", "COLLEGE"));
        
        // 总浏览量
        QueryWrapper<Article> viewWrapper = new QueryWrapper<>();
        viewWrapper.eq("is_approved", 1).select("IFNULL(SUM(view_count), 0) as total_views");
        Map<String, Object> viewResult = articleMapper.selectMaps(viewWrapper).stream().findFirst().orElse(new HashMap<>());
        Object totalViews = viewResult.get("total_views");
        
        StringBuilder sb = new StringBuilder("【系统统计数据】：\n");
        sb.append(String.format("- 文章总数：%d 篇\n", totalArticles));
        sb.append(String.format("- 官方新闻：%d 篇\n", officialCount));
        sb.append(String.format("- 全校新闻：%d 篇\n", campusCount));
        sb.append(String.format("- 学院新闻：%d 篇\n", collegeCount));
        sb.append(String.format("- 总浏览量：%s 次\n", totalViews != null ? totalViews.toString() : "0"));
        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * 获取指定类型的文章
     */
    private String getArticlesByType(String boardType, String typeName) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("is_approved", 1)
               .eq("board_type", boardType)
               .orderByDesc("created_at")
               .last("LIMIT 5");
        List<Article> articles = articleMapper.selectList(wrapper);
        
        if (articles.isEmpty()) {
            return String.format("【%s】：暂无数据\n\n", typeName);
        }
        
        StringBuilder sb = new StringBuilder(String.format("【最新%s TOP5】：\n", typeName));
        for (int i = 0; i < articles.size(); i++) {
            Article a = articles.get(i);
            sb.append(String.format("%d. 《%s》 - 浏览量：%d\n", 
                i + 1, a.getTitle(), a.getViewCount()));
        }
        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * 获取置顶文章
     */
    private String getPinnedArticles() {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("is_approved", 1)
               .eq("is_pinned", 1)
               .orderByDesc("created_at");
        List<Article> articles = articleMapper.selectList(wrapper);
        
        if (articles.isEmpty()) {
            return "【置顶文章】：当前没有置顶文章\n\n";
        }
        
        StringBuilder sb = new StringBuilder("【置顶文章】：\n");
        for (int i = 0; i < articles.size(); i++) {
            Article a = articles.get(i);
            sb.append(String.format("%d. 《%s》 - 板块：%s，浏览量：%d\n", 
                i + 1, a.getTitle(), getBoardTypeName(a.getBoardType()), a.getViewCount()));
        }
        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * 获取用户统计
     */
    private String getUserStatistics() {
        long totalUsers = userMapper.selectCount(new QueryWrapper<>());
        
        StringBuilder sb = new StringBuilder("【用户统计】：\n");
        sb.append(String.format("- 注册用户总数：%d 人\n", totalUsers));
        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * 获取板块类型中文名
     */
    private String getBoardTypeName(String boardType) {
        if (boardType == null) return "未知";
        return switch (boardType) {
            case "OFFICIAL" -> "官方新闻";
            case "CAMPUS" -> "全校新闻";
            case "COLLEGE" -> "学院新闻";
            default -> boardType;
        };
    }
    
    /**
     * 获取模型显示名称
     */
    private String getModelDisplayName(String modelType) {
        return switch (modelType.toLowerCase()) {
            case "kimi" -> "Kimi（月之暗面 Moonshot AI）";
            case "deepseek" -> "DeepSeek（深度求索）";
            case "doubao" -> "豆包（字节跳动）";
            default -> modelType;
        };
    }
    
    /**
     * 构建系统提示词（包含系统详细信息）
     */
    private String buildSystemPrompt(String modelType) {
        String modelInfo = getModelDisplayName(modelType);
        return """
            # 🎓 WHUTGPT - 校园新闻智能助手
            
            你是「武汉理工大学校园新闻发布系统」的专属AI助手，名叫「WHUTGPT」。你友善、专业、乐于助人，是校园师生获取资讯和使用系统的得力帮手。
            
            ## 🤖 关于你自己
            - **名称**：WHUTGPT（武理智能助手）
            - **底层模型**：""" + modelInfo + """
            - **特长**：校园新闻检索、系统功能引导、内容创作辅助、数据统计分析
            - 当用户询问你是什么模型、用的什么AI时，请如实告知底层模型信息
            
            ## 📚 系统功能全景
            
            ### 一、新闻板块（三大分类）
            | 板块 | 说明 | 发布权限 |
            |------|------|----------|
            | 🏛️ 官方新闻 | 学校官方通知、政策解读、重要公告 | 管理员、教师 |
            | 🎪 全校新闻 | 全校范围的活动、赛事、讲座等 | 所有登录用户 |
            | 🏫 学院新闻 | 各学院的特色活动和通知 | 需绑定对应学院 |
            
            ### 二、用户角色与权限
            - **学生**：浏览新闻、发布全校/学院新闻、评论、点赞、收藏、关注用户
            - **教师**：在学生权限基础上，可发布官方新闻
            - **管理员**：拥有全部权限，可审核文章、管理用户、置顶/推荐文章、管理学院
            
            ### 三、核心功能
            1. **首页浏览**：查看最新/最热资讯，支持分类筛选和排序（日期/热度）
            2. **文章发布**：支持富文本编辑、封面图上传、板块选择
            3. **文章详情**：阅读全文、查看评论、点赞收藏、关注作者
            4. **个人中心**：管理个人信息、查看我的文章/收藏/草稿
            5. **关注系统**：关注感兴趣的作者，查看关注动态，发现推荐用户
            6. **通知中心**：接收评论、点赞、关注等消息通知
            7. **搜索功能**：按关键词搜索文章
            8. **管理后台**（管理员专属）：用户管理、文章审核、学院管理、数据统计
            
            ### 四、操作指南
            
            **📝 发布文章流程：**
            1. 登录系统 → 2. 点击顶部「发布」按钮 → 3. 填写标题（必填）和摘要 → 4. 编写正文 → 5. 上传封面图（可选）→ 6. 选择发布板块 → 7. 点击发布
            
            **🔍 搜索文章方法：**
            - 使用首页搜索框输入关键词
            - 或直接告诉我"搜索xxx"，我会帮你查询相关文章
            
            **👥 关注用户方法：**
            点击文章作者头像 → 进入作者主页 → 点击「关注」按钮
            
            **🔔 查看通知：**
            点击顶部导航栏的铃铛图标，可查看评论、点赞、新粉丝等通知
            
            ## 🔮 我的数据能力
            
            我可以实时查询系统数据库，为你提供准确的统计信息。你可以问我：
            - 📊 **统计类**：系统有多少文章？多少用户？总浏览量？
            - 🔥 **热门类**：最热门的文章是什么？浏览量最高的文章？
            - 🆕 **最新类**：最新发布的文章？今天有什么新闻？
            - 🏷️ **分类类**：官方新闻有哪些？学院新闻有什么？
            - 📌 **推荐类**：有哪些置顶文章？推荐阅读什么？
            - ⭐ **社交类**：粉丝最多的用户是谁？平台关注数据？
            - 🔍 **搜索类**：帮我搜索关于xxx的新闻
            
            ## ✍️ 写作辅助能力
            
            我可以帮助你：
            - 📝 **新闻稿撰写**：提供标题、摘要和正文框架
            - 🎯 **文案优化**：让文章更有吸引力
            - 📋 **摘要生成**：为长文自动生成精炼摘要
            - 💡 **创意建议**：提供选题和写作方向建议
            - 📧 **邮件模板**：撰写正式或非正式邮件
            - 🎤 **演讲稿**：撰写开场白、主体和结尾
            - 📝 **活动策划**：提供活动方案框架
            - 🎨 **海报文案**：创意宣传语和标语
            
            ## 📚 学习辅助能力
            
            我可以帮助你：
            - 📖 **知识解答**：解答学习中的疑问
            - 📋 **学习计划**：制定合理的学习安排
            - 💡 **学习方法**：分享高效学习技巧
            - 📝 **论文指导**：提供论文写作建议
            - 🎯 **考试技巧**：分享应试经验和方法
            - 🌐 **英语学习**：翻译、语法、写作指导
            
            ## 🎓 校园生活指南
            
            我可以提供：
            - 🏫 **校园导览**：介绍校园设施和服务
            - 🍽️ **生活建议**：食堂、宿舍、图书馆使用攻略
            - 👥 **社团推荐**：介绍各类学生组织
            - 💼 **职业规划**：实习、求职、考研建议
            - 🎭 **活动信息**：校园文化活动介绍
            
            ## 💬 回答原则
            
            1. **准确性**：基于系统实际数据回答，不编造虚假信息
            2. **实用性**：优先提供可操作的具体指导
            3. **简洁性**：回答精炼，重点突出，避免冗长
            4. **友好性**：语气亲切自然，适当使用emoji增添活力
            5. **引导性**：巧妙引导用户探索系统功能
            
            ## 🎯 回答格式建议
            
            - 列表回答使用简洁的项目符号
            - 步骤说明使用数字编号
            - 重要信息适当加粗或使用emoji标注
            - 涉及文章时提供访问链接 /article/{id}
            - 回答末尾可提供相关建议或追问引导
            
            ## 📍 常见问题快速回复模板
            
            **Q: 如何发布文章？**
            A: 登录后点击顶部「发布」按钮，填写标题和内容，选择板块后即可发布！官方新闻需教师/管理员权限，学院新闻需绑定学院。
            
            **Q: 怎么关注别人？**
            A: 点击任意文章进入详情页，点击作者头像旁的「关注」按钮即可。关注后可在「关注」页面查看TA的动态。
            
            **Q: 忘记密码怎么办？**
            A: 目前请联系管理员重置密码，或使用注册邮箱找回。
            
            **Q: 文章为什么不显示？**
            A: 可能原因：1)文章待审核 2)已被管理员下架 3)网络问题。请检查个人中心的文章状态。
            
            ## ⚠️ 注意事项
            
            - 如果用户问题超出校园新闻系统范围，可以正常回答，但尽量自然地关联回系统功能
            - 不确定的信息请如实告知，不要编造
            - 涉及敏感话题请婉转处理，引导正向交流
            - 鼓励用户积极参与校园新闻的创作和互动
            """;
    }
    
    /**
     * 生成备用回复（当 API 调用失败时使用）
     */
    private String generateFallbackResponse(String prompt) {
        if (prompt.contains("你好") || prompt.contains("您好")) {
            return "你好！我是校园新闻助手。目前AI服务暂时不可用，但我仍然可以为您提供基本帮助。请问有什么可以帮您的？";
        }
        return "抱歉，AI 服务暂时遇到问题。请稍后再试，或联系管理员。";
    }
    
    /**
     * 获取关注统计数据
     */
    private String getFollowStatistics() {
        StringBuilder sb = new StringBuilder("\n【关注系统统计】\n");
        
        // 总关注关系数
        Long totalFollows = userFollowMapper.selectCount(null);
        sb.append("- 平台总关注关系数: ").append(totalFollows).append("\n");
        
        // 有粉丝的用户数
        QueryWrapper<User> hasFollowerWrapper = new QueryWrapper<>();
        hasFollowerWrapper.gt("follower_count", 0);
        Long usersWithFollowers = userMapper.selectCount(hasFollowerWrapper);
        sb.append("- 有粉丝的用户数: ").append(usersWithFollowers).append("\n");
        
        // 平均粉丝数
        if (usersWithFollowers > 0) {
            sb.append("- 平均每人关注数: ").append(totalFollows / usersWithFollowers).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 获取粉丝最多的用户排行
     */
    private String getTopFollowedUsers() {
        StringBuilder sb = new StringBuilder("\n【粉丝排行榜 TOP5】\n");
        
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.gt("follower_count", 0)
               .orderByDesc("follower_count")
               .last("LIMIT 5");
        
        List<User> topUsers = userMapper.selectList(wrapper);
        
        int rank = 1;
        for (User user : topUsers) {
            sb.append(rank).append(". ")
              .append(user.getRealName() != null ? user.getRealName() : user.getUsername())
              .append(" - 粉丝数: ").append(user.getFollowerCount() != null ? user.getFollowerCount() : 0)
              .append(", 关注数: ").append(user.getFollowingCount() != null ? user.getFollowingCount() : 0)
              .append("\n");
            rank++;
        }
        
        if (topUsers.isEmpty()) {
            sb.append("暂无粉丝数据\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 提取搜索关键词
     * 支持多种搜索表达方式
     */
    private String extractSearchKeyword(String prompt) {
        // 常见的搜索表达方式
        String[] searchPatterns = {
            "搜索", "查找", "找一下", "搜一下", "查一下", 
            "帮我找", "帮我搜", "有没有关于", "有什么关于",
            "想看", "想了解", "了解一下", "查询"
        };
        
        String lowerPrompt = prompt.toLowerCase();
        
        for (String pattern : searchPatterns) {
            if (lowerPrompt.contains(pattern)) {
                // 提取关键词 - 移除搜索指令词，保留实际搜索内容
                String keyword = prompt;
                
                // 移除常见前缀
                String[] prefixes = {"帮我搜索", "帮我查找", "帮我找", "帮我搜", 
                    "搜索一下", "查找一下", "找一下", "搜一下", "查一下",
                    "搜索", "查找", "查询",
                    "有没有关于", "有什么关于", "关于",
                    "想看看", "想看", "想了解", "了解一下",
                    "请搜索", "请查找", "请找"};
                
                for (String prefix : prefixes) {
                    if (keyword.startsWith(prefix)) {
                        keyword = keyword.substring(prefix.length());
                        break;
                    }
                }
                
                // 移除常见后缀
                String[] suffixes = {"的新闻", "的文章", "的资讯", "的内容", 
                    "新闻", "文章", "资讯", "内容", "相关", "吧", "吗", "呢", "啊"};
                
                for (String suffix : suffixes) {
                    if (keyword.endsWith(suffix)) {
                        keyword = keyword.substring(0, keyword.length() - suffix.length());
                    }
                }
                
                keyword = keyword.trim();
                
                // 如果关键词不为空且长度合理，返回关键词
                if (!keyword.isEmpty() && keyword.length() >= 2 && keyword.length() <= 50) {
                    return keyword;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 搜索文章
     * 根据关键词搜索标题和内容
     */
    private String searchArticles(String keyword) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("is_approved", 1)
               .and(w -> w.like("title", keyword).or().like("content", keyword))
               .orderByDesc("view_count")
               .last("LIMIT 5");
        
        List<Article> articles = articleMapper.selectList(wrapper);
        
        if (articles.isEmpty()) {
            return String.format("\n【搜索结果】关键词“%s”\n未找到相关文章。建议您：\n" +
                    "1. 尝试使用不同的关键词\n" +
                    "2. 使用更简短的词语\n" +
                    "3. 检查是否有错别字\n\n", keyword);
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n【搜索结果】关键词“%s”，共找到 %d 篇相关文章：\n\n", keyword, articles.size()));
        
        for (int i = 0; i < articles.size(); i++) {
            Article a = articles.get(i);
            String time = a.getCreatedAt() != null ? a.getCreatedAt().format(formatter) : "未知";
            String summary = a.getSummary();
            if (summary == null || summary.isEmpty()) {
                summary = a.getContent();
                if (summary != null && summary.length() > 60) {
                    summary = summary.substring(0, 60) + "...";
                }
            } else if (summary.length() > 60) {
                summary = summary.substring(0, 60) + "...";
            }
            
            sb.append(String.format("%d. 《%s》\n", i + 1, a.getTitle()));
            sb.append(String.format("   📌 板块：%s | 👁️ 浏览：%d | 📅 %s\n", 
                getBoardTypeName(a.getBoardType()), a.getViewCount(), time));
            sb.append(String.format("   📝 %s\n", summary != null ? summary : "暂无摘要"));
            sb.append(String.format("   🔗 文章ID：%d，访问链接：/article/%d\n\n", a.getId(), a.getId()));
        }
        
        sb.append("提示：点击文章链接即可查看详情。\n");
        
        return sb.toString();
    }
    
    /**
     * 缓存对话记录到 Redis
     */
    private void cacheConversation(String sessionId, String question, String answer) {
        try {
            String key = AI_CHAT_CACHE_PREFIX + sessionId;
            String conversation = String.format("Q: %s\nA: %s\n---\n", question, answer);
            
            // 追加到现有对话
            Object existing = redisTemplate.opsForValue().get(key);
            String history = existing != null ? existing.toString() + conversation : conversation;
            
            redisTemplate.opsForValue().set(key, history, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.debug("对话已缓存，sessionId: {}", sessionId);
        } catch (Exception e) {
            // Redis 不可用时不影响主流程
            log.warn("缓存对话失败: {}", e.getMessage());
        }
    }
    
    /**
     * 获取历史对话（可选功能）
     */
    public String getConversationHistory(String sessionId) {
        try {
            String key = AI_CHAT_CACHE_PREFIX + sessionId;
            Object history = redisTemplate.opsForValue().get(key);
            return history != null ? history.toString() : "";
        } catch (Exception e) {
            log.warn("获取对话历史失败: {}", e.getMessage());
            return "";
        }
    }
    
    /**
     * 文章智能推荐（预留）
     */
    public void recommendArticles(Long userId) {
        // TODO: 实现AI推荐逻辑
    }
    
    /**
     * 自动生成摘要（预留）
     */
    public String generateSummary(String content) {
        // TODO: 接入AI生成摘要
        return callLLM("请为以下内容生成一个简洁的摘要：\n" + content);
    }
    
    /**
     * 内容审核（预留）
     */
    public boolean moderateContent(String content) {
        // TODO: AI内容审核
        return true;
    }
}
