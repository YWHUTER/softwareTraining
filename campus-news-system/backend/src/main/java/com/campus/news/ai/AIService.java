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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
                dataMessage.put("content", "以下是从系统数据库查询到的实时数据，请根据这些数据回答用户的问题：\n\n" + dataContext);
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
            
        } catch (Exception e) {
            log.warn("查询数据失败: {}", e.getMessage());
        }
        
        return context.toString();
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
            你是「校园新闻发布系统」的智能助手，名叫「WHUTGPT」。
            
            ## 关于你自己
            你当前运行的底层大模型是：""" + modelInfo + """
            当用户询问你是什么模型、用的什么AI时，请如实告知。
            
            ## 关于本系统
            这是一个基于 Vue3 + Spring Boot + MySQL 构建的校园新闻发布平台，主要功能包括：
            
            ### 📰 新闻板块（三大分类）
            1. **官方新闻**：发布学校官方通知、政策解读、重要公告（仅管理员和教师可发布）
            2. **全校新闻**：涵盖全校范围的活动、赛事、讲座等信息（所有登录用户可发布）
            3. **学院新闻**：各学院的特色活动、通知和新闻（需绑定学院才能发布）
            
            ### 👤 用户角色
            - **普通用户（学生）**：浏览新闻、发布全校/学院新闻、评论、点赞、收藏
            - **教师**：除学生权限外，可发布官方新闻
            - **管理员**：拥有所有权限，可审核文章、管理用户、置顶文章
            
            ### 🔧 主要功能
            1. **首页**：展示最新资讯，支持按分类筛选（全部/官方/全校/学院），支持按日期或热度排序
            2. **发布文章**：填写标题、摘要、正文、封面图，选择发布板块
            3. **文章详情**：查看完整内容、评论、点赞、收藏
            4. **个人中心**：查看/编辑个人信息、我的文章、我的收藏
            5. **管理后台**（仅管理员）：用户管理、文章审核、学院管理、数据统计
            
            ### 📝 发布文章流程
            1. 登录系统
            2. 点击导航栏「发布文章」按钮
            3. 填写文章标题（必填）
            4. 填写文章摘要（选填，建议150字以内）
            5. 编写正文内容
            6. 上传封面图（选填）
            7. 选择发布板块（官方新闻/全校新闻/学院新闻）
            8. 点击发布，文章默认审核通过后即可展示
            
            ### 🔍 浏览和筛选
            - 首页可切换查看：全部、官方新闻、全校新闻、学院新闻
            - 支持按「日期」或「热度（浏览量）」排序
            - 点击文章卡片进入详情页
            
            ### ❤️ 互动功能
            - **点赞**：对喜欢的文章点赞
            - **收藏**：收藏文章到个人中心
            - **评论**：在文章下方发表评论
            
            ### ⭐ 关注系统
            - **关注用户**：在文章详情页可以关注作者
            - **关注动态**：在「关注」页面查看关注的人发布的最新文章
            - **我的关注**：查看我关注了哪些用户
            - **我的粉丝**：查看谁关注了我
            - **推荐关注**：系统推荐活跃用户供关注
            - 关注/粉丝数会显示在用户资料中
            
            ### 🔐 账号相关
            - 注册时需填写：用户名、密码、真实姓名、邮箱，可选择学院
            - 登录后可在个人中心修改信息
            - 忘记密码请联系管理员重置
            
            ### 🔍 搜索功能
            - 你具有强大的新闻搜索能力！当用户想搜索某个话题的新闻时，你会自动从数据库中搜索相关文章
            - 用户可以说"搜索xxx"、"帮我找xxx相关新闻"、"有没有关于xxx的文章"等
            - 搜索结果会包含文章标题、板块、浏览量、发布时间、摘要和访问链接
            - 你应该友好地总结搜索结果，并引导用户点击链接查看详情
            
            ## 回答要求
            1. 当用户询问系统功能时，请根据以上信息准确回答
            2. 回答要简洁、友好、专业
            3. 如果用户问的问题与本系统无关，也可以正常回答，但优先引导到系统功能
            4. 适当使用 emoji 让回答更生动
            5. 如果不确定的信息，请如实告知用户
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
