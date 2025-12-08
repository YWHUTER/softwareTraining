package com.campus.news.agent;

import com.campus.news.agent.tools.NewsAgentTools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 新闻系统智能Agent服务
 * 基于LangChain4j框架，提供智能化的任务执行能力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsAgentService {
    
    private final NewsAgentTools agentTools;
    
    // 存储会话记忆
    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();
    
    // LLM配置（可以从配置文件读取）
    @Value("${agent.llm.api-key:sk-4hnnoqUMCqXuGIkZ1mwAZHv2RWDFbSeP4WHQWrtosP0FwIGw}")
    private String apiKey;
    
    @Value("${agent.llm.base-url:https://api.moonshot.cn/v1}")
    private String baseUrl;
    
    @Value("${agent.llm.model:moonshot-v1-8k}")
    private String modelName;
    
    private ChatLanguageModel chatModel;
    private NewsAgent newsAgent;
    
    /**
     * Agent接口定义
     * 使用LangChain4j的AiServices自动生成代理实现
     */
    public interface NewsAgent {
        @SystemMessage("""
            你是一个智能的校园新闻系统助手Agent。你可以帮助用户完成以下任务：
            
            1. 搜索和查询：搜索文章、获取热门文章、查看最新发布
            2. 数据分析：统计系统数据、用户排行榜、分类统计
            3. 内容创作：创建文章草稿、生成文章摘要
            4. 系统管理：查看分类列表、用户信息等
            
            当用户提出需求时，你应该：
            - 理解用户意图，选择合适的工具完成任务
            - 如果任务需要多步骤，请按顺序执行
            - 以友好、专业的方式回应用户
            - 在执行任务后，提供清晰的结果总结
            
            注意：
            - 你可以同时调用多个工具来完成复杂任务
            - 如果用户的需求不明确，请主动询问补充信息
            - 始终保持专业和友好的语气
            """)
        String chat(@MemoryId String sessionId, @UserMessage String userMessage);
    }
    
    @PostConstruct
    public void init() {
        // 初始化LLM模型（兼容OpenAI API的模型）
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.7)
                .maxTokens(2000)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
        
        // 创建Agent服务
        this.newsAgent = AiServices.builder(NewsAgent.class)
                .chatLanguageModel(chatModel)
                .chatMemoryProvider(sessionId -> getOrCreateMemory(sessionId.toString()))
                .tools(agentTools)  // 注入工具类
                .build();
        
        log.info("NewsAgent初始化完成，已加载工具类");
    }
    
    /**
     * 执行Agent任务
     * @param request Agent请求
     * @return Agent响应
     */
    public AgentResponse executeTask(AgentRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        log.info("Agent开始执行任务 - 会话: {}, 消息: {}", sessionId, request.getMessage());
        
        AgentResponse response = new AgentResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        
        try {
            // 记录开始时间
            long startTime = System.currentTimeMillis();
            
            // 调用Agent处理消息
            String result = newsAgent.chat(sessionId, request.getMessage());
            
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            response.setResult(result);
            response.setExecutionTime(executionTime);
            
            // 获取执行的步骤（简化处理）
            List<AgentStep> steps = new ArrayList<>();
            steps.add(new AgentStep("理解意图", "分析用户需求：" + request.getMessage(), true));
            steps.add(new AgentStep("执行任务", "调用相关工具完成任务", true));
            steps.add(new AgentStep("生成结果", "整理并返回执行结果", true));
            response.setSteps(steps);
            
            log.info("Agent任务执行成功，耗时: {}ms", executionTime);
            
        } catch (Exception e) {
            log.error("Agent任务执行失败", e);
            response.setSuccess(false);
            response.setError("任务执行失败：" + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 流式执行Agent任务（支持实时反馈）
     * @param request Agent请求
     * @param callback 回调函数
     */
    public void executeTaskStream(AgentRequest request, AgentStreamCallback callback) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        log.info("Agent开始流式执行任务 - 会话: {}", sessionId);
        
        try {
            // 发送开始事件
            callback.onStart(sessionId);
            
            // 发送思考步骤
            callback.onStep("理解意图", "正在分析您的需求...");
            Thread.sleep(500); // 模拟处理
            
            // 执行Agent
            callback.onStep("执行任务", "正在调用相关工具...");
            String result = newsAgent.chat(sessionId, request.getMessage());
            
            // 发送结果
            callback.onStep("生成结果", "任务执行完成");
            callback.onResult(result);
            
            // 完成
            callback.onComplete();
            
        } catch (Exception e) {
            log.error("Agent流式任务执行失败", e);
            callback.onError("执行失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取或创建会话记忆
     */
    private ChatMemory getOrCreateMemory(String sessionId) {
        return sessionMemories.computeIfAbsent(sessionId, 
            id -> MessageWindowChatMemory.withMaxMessages(10));
    }
    
    /**
     * 清除会话记忆
     */
    public void clearSession(String sessionId) {
        sessionMemories.remove(sessionId);
        log.info("清除会话记忆: {}", sessionId);
    }
    
    /**
     * 获取所有可用工具
     */
    public List<ToolInfo> getAvailableTools() {
        List<ToolInfo> tools = new ArrayList<>();
        
        tools.add(new ToolInfo("searchArticles", "搜索新闻文章", 
            "根据关键词搜索文章标题和内容"));
        tools.add(new ToolInfo("getHotArticles", "获取热门文章", 
            "获取浏览量最高的文章排行榜"));
        tools.add(new ToolInfo("getSystemStats", "系统统计", 
            "获取文章总数、用户数、浏览量等统计数据"));
        tools.add(new ToolInfo("createArticleDraft", "创建文章草稿", 
            "创建新的文章草稿"));
        tools.add(new ToolInfo("getUserRanking", "用户排行榜", 
            "获取粉丝或文章数排行榜"));
        tools.add(new ToolInfo("getLatestArticles", "最新文章", 
            "获取最新发布的文章"));
        tools.add(new ToolInfo("getCategories", "分类列表", 
            "获取所有文章分类"));
        
        return tools;
    }
    
    // ===== 内部类定义 =====
    
    /**
     * Agent请求
     */
    @Data
    public static class AgentRequest {
        private String sessionId;
        private String message;
        private Map<String, Object> context;
    }
    
    /**
     * Agent响应
     */
    @Data
    public static class AgentResponse {
        private String sessionId;
        private boolean success;
        private String result;
        private String error;
        private List<AgentStep> steps;
        private Long executionTime; // 执行时间(毫秒)
    }
    
    /**
     * Agent执行步骤
     */
    @Data
    public static class AgentStep {
        private String name;
        private String description;
        private boolean completed;
        private String result;
        
        public AgentStep(String name, String description, boolean completed) {
            this.name = name;
            this.description = description;
            this.completed = completed;
        }
    }
    
    /**
     * 工具信息
     */
    @Data
    public static class ToolInfo {
        private String name;
        private String displayName;
        private String description;
        
        public ToolInfo(String name, String displayName, String description) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
        }
    }
    
    /**
     * 流式回调接口
     */
    public interface AgentStreamCallback {
        void onStart(String sessionId);
        void onStep(String step, String description);
        void onResult(String result);
        void onError(String error);
        void onComplete();
    }
}
