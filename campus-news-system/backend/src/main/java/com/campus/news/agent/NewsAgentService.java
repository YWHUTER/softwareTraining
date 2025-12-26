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
 * ============================================================================
 * 新闻系统智能Agent服务 (NewsAgentService)
 * ============================================================================
 * 
 * 【这是什么？- 答辩核心！】
 * 这是一个基于LangChain4j框架实现的AI Agent（智能代理）。
 * Agent可以理解用户的自然语言指令，自动调用各种工具完成任务。
 * 
 * 【什么是Agent？】
 * Agent = 大语言模型(LLM) + 工具(Tools) + 记忆(Memory)
 * - LLM：理解用户意图，决定调用哪个工具
 * - Tools：具体执行任务的方法（如搜索文章、发布内容）
 * - Memory：记住对话历史，支持多轮对话
 * 
 * 【工作流程示例】
 * 用户说："帮我搜索关于校园活动的新闻"
 * 1. Agent理解意图：用户想搜索文章
 * 2. Agent选择工具：调用searchArticles工具
 * 3. Agent执行工具：传入关键词"校园活动"
 * 4. Agent整理结果：将搜索结果组织成自然语言回复
 * 
 * 【技术栈】
 * - LangChain4j：Java版的LangChain框架，用于构建AI应用
 * - Kimi API：月之暗面的大语言模型，兼容OpenAI接口
 * - Spring Boot：后端框架
 * 
 * 【答辩要点】
 * Q: 为什么要用Agent而不是直接调用AI？
 * A: Agent可以自动调用工具执行任务，比如搜索数据库、发布文章，
 *    而普通AI只能聊天，不能操作系统
 * 
 * Q: LangChain4j是什么？
 * A: 是一个Java框架，帮助开发者快速构建AI应用，
 *    提供了Agent、Memory、Tools等组件
 * 
 * Q: 为什么用Kimi而不是ChatGPT？
 * A: Kimi是国产模型，访问稳定，且兼容OpenAI接口，切换成本低
 */
@Slf4j  // Lombok：自动生成日志对象
@Service  // Spring：标记为服务组件
@RequiredArgsConstructor  // Lombok：自动生成构造函数
public class NewsAgentService {
    
    // ==================== 依赖注入 ====================
    
    /**
     * Agent工具类 - 包含所有Agent可以调用的工具方法
     * 如：搜索文章、发布内容、获取统计数据等
     */
    private final NewsAgentTools agentTools;
    
    // ==================== 会话记忆管理 ====================
    
    /**
     * 会话记忆存储
     * 
     * 【作用】
     * 存储每个用户的对话历史，实现多轮对话
     * 比如用户说"继续搜索"，Agent能知道之前搜索的是什么
     * 
     * 【数据结构】
     * Key: sessionId（会话ID）
     * Value: ChatMemory（对话记忆对象）
     * 
     * 【为什么用ConcurrentHashMap？】
     * 支持多线程并发访问，多个用户同时使用不会冲突
     */
    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();
    
    // ==================== LLM配置（从配置文件读取）====================
    
    /**
     * API密钥 - 用于调用Kimi大模型
     * 配置在application.yml中：agent.llm.api-key
     */
    @Value("${agent.llm.api-key:sk-4hnnoqUMCqXuGIkZ1mwAZHv2RWDFbSeP4WHQWrtosP0FwIGw}")
    private String apiKey;
    
    /**
     * API基础URL - Kimi的API地址
     * Kimi兼容OpenAI接口，所以可以用OpenAI的SDK调用
     */
    @Value("${agent.llm.base-url:https://api.moonshot.cn/v1}")
    private String baseUrl;
    
    /**
     * 模型名称
     * moonshot-v1-8k：8K上下文窗口，适合普通对话
     * moonshot-v1-32k：32K上下文，适合长文本
     * moonshot-v1-128k：128K上下文，适合超长文档
     */
    @Value("${agent.llm.model:moonshot-v1-8k}")
    private String modelName;
    
    // ==================== Agent核心组件 ====================
    
    private ChatLanguageModel chatModel;  // 大语言模型客户端
    private NewsAgent newsAgent;          // Agent代理对象
    
    // ==================== Agent接口定义 ====================
    
    /**
     * Agent接口定义
     * 
     * 【这是什么？】
     * 这是一个Java接口，LangChain4j会自动生成它的实现类。
     * 我们只需要定义接口方法和系统提示词，框架会自动：
     * 1. 调用大模型理解用户意图
     * 2. 选择合适的工具执行任务
     * 3. 整理结果返回给用户
     * 
     * 【@SystemMessage注解】
     * 定义Agent的"人设"和能力范围，告诉AI它是谁、能做什么
     * 这段提示词会在每次对话时发送给大模型
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
        // @MemoryId: 标记会话ID参数，用于区分不同用户的对话
        // @UserMessage: 标记用户消息参数
    }
    
    // ==================== 初始化方法 ====================
    
    /**
     * 初始化Agent
     * 
     * 【@PostConstruct注解】
     * Spring容器创建Bean后自动调用此方法
     * 用于初始化大模型客户端和Agent代理
     * 
     * 【初始化流程】
     * 1. 创建ChatLanguageModel：配置API地址、密钥、模型参数
     * 2. 创建NewsAgent代理：绑定模型、记忆、工具
     */
    @PostConstruct
    public void init() {
        // ========== 第一步：初始化大语言模型客户端 ==========
        // 使用OpenAI兼容接口，因为Kimi的API格式与OpenAI相同
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)                    // API密钥
                .baseUrl(baseUrl)                  // API地址（Kimi的地址）
                .modelName(modelName)              // 模型名称
                .temperature(0.7)                  // 温度参数：0-1，越高回复越随机
                .maxTokens(2000)                   // 最大输出token数
                .timeout(Duration.ofSeconds(60))  // 超时时间60秒
                .logRequests(true)                 // 记录请求日志（调试用）
                .logResponses(true)                // 记录响应日志（调试用）
                .build();
        
        // ========== 第二步：创建Agent服务 ==========
        // AiServices是LangChain4j的核心类，用于创建Agent代理
        this.newsAgent = AiServices.builder(NewsAgent.class)
                .chatLanguageModel(chatModel)      // 绑定大模型
                .chatMemoryProvider(sessionId -> getOrCreateMemory(sessionId.toString()))  // 绑定记忆
                .tools(agentTools)                 // 绑定工具类（关键！）
                .build();
        
        log.info("NewsAgent初始化完成，已加载工具类");
    }
    
    // ==================== 核心执行方法 ====================
    
    /**
     * 执行Agent任务（同步方式）
     * 
     * 【这是Agent的核心入口方法！】
     * 
     * 【执行流程】
     * 1. 生成或获取会话ID
     * 2. 设置用户上下文（用于权限控制）
     * 3. 调用Agent处理用户消息
     * 4. 记录执行步骤和时间
     * 5. 返回执行结果
     * 
     * 【权限控制】
     * 通过NewsAgentTools.setCurrentUser()设置当前用户信息，
     * 工具方法可以根据用户身份决定是否允许执行某些操作
     * 
     * @param request Agent请求，包含消息、用户ID、是否管理员等
     * @return Agent响应，包含执行结果、步骤、耗时等
     */
    public AgentResponse executeTask(AgentRequest request) {
        // 获取或生成会话ID
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        log.info("Agent开始执行任务 - 会话: {}, 消息: {}, 用户ID: {}, 是否管理员: {}", 
                 sessionId, request.getMessage(), request.getUserId(), request.getIsAdmin());
        
        // 构建响应对象
        AgentResponse response = new AgentResponse();
        response.setSessionId(sessionId);
        response.setSuccess(true);
        
        try {
            // ========== 设置用户上下文（权限控制）==========
            // 这样工具方法就能知道当前是谁在操作
            NewsAgentTools.setCurrentUser(request.getUserId(), request.getIsAdmin());
            
            // 记录开始时间
            long startTime = System.currentTimeMillis();
            
            // ========== 调用Agent处理消息（核心！）==========
            // newsAgent.chat()会：
            // 1. 将消息发送给大模型
            // 2. 大模型分析意图，决定调用哪个工具
            // 3. 执行工具方法
            // 4. 将结果整理成自然语言返回
            String result = newsAgent.chat(sessionId, request.getMessage());
            
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            response.setResult(result);
            response.setExecutionTime(executionTime);
            
            // 记录执行步骤（简化处理，实际可以更详细）
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
        } finally {
            // ========== 清除用户上下文 ==========
            // 重要！防止内存泄漏和权限混乱
            NewsAgentTools.clearCurrentUser();
        }
        
        return response;
    }
    
    // ==================== 流式执行方法 ====================
    
    /**
     * 流式执行Agent任务（支持实时反馈）
     * 
     * 【什么是流式执行？】
     * 普通执行：等Agent完全处理完才返回结果
     * 流式执行：边处理边返回，用户能看到实时进度
     * 
     * 【使用场景】
     * 当任务执行时间较长时，通过流式反馈让用户知道系统在工作，
     * 提升用户体验
     * 
     * @param request  Agent请求
     * @param callback 回调函数，用于实时推送执行状态
     */
    public void executeTaskStream(AgentRequest request, AgentStreamCallback callback) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        
        log.info("Agent开始流式执行任务 - 会话: {}, 用户ID: {}, 是否管理员: {}", 
                 sessionId, request.getUserId(), request.getIsAdmin());
        
        try {
            // 设置用户上下文
            NewsAgentTools.setCurrentUser(request.getUserId(), request.getIsAdmin());
            
            // 发送开始事件
            callback.onStart(sessionId);
            
            // 发送思考步骤（让用户知道Agent在工作）
            callback.onStep("理解意图", "正在分析您的需求...");
            Thread.sleep(500); // 模拟处理时间
            
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
        } finally {
            NewsAgentTools.clearCurrentUser();
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 获取或创建会话记忆
     * 
     * 【记忆机制说明】
     * 每个会话（sessionId）有独立的记忆，存储最近10条消息。
     * 这样Agent可以理解上下文，支持多轮对话。
     * 
     * 例如：
     * 用户："搜索校园活动"
     * Agent：返回搜索结果
     * 用户："第一篇文章的详情"  <- Agent能理解"第一篇"指的是什么
     * 
     * @param sessionId 会话ID
     * @return 会话记忆对象
     */
    private ChatMemory getOrCreateMemory(String sessionId) {
        return sessionMemories.computeIfAbsent(sessionId, 
            id -> MessageWindowChatMemory.withMaxMessages(10));  // 最多保留10条消息
    }
    
    /**
     * 清除会话记忆
     * 
     * 【使用场景】
     * 1. 用户主动清除对话历史
     * 2. 会话超时自动清理
     * 3. 用户退出登录
     * 
     * @param sessionId 会话ID
     */
    public void clearSession(String sessionId) {
        sessionMemories.remove(sessionId);
        log.info("清除会话记忆: {}", sessionId);
    }
    
    /**
     * 获取所有可用工具列表
     * 
     * 【用途】
     * 前端可以调用这个方法展示Agent的能力，
     * 让用户知道可以让Agent做什么
     * 
     * @return 工具信息列表
     */
    public List<ToolInfo> getAvailableTools() {
        List<ToolInfo> tools = new ArrayList<>();
        
        // 这里列出主要工具，实际工具在NewsAgentTools类中定义
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
    
    // ==================== 内部类定义 ====================
    
    /**
     * Agent请求对象
     * 
     * 【字段说明】
     * - sessionId: 会话ID，用于关联多轮对话
     * - message: 用户发送的消息
     * - userId: 当前用户ID，用于权限控制
     * - isAdmin: 是否管理员，决定能执行哪些操作
     * - context: 额外上下文信息（扩展用）
     */
    @Data  // Lombok：自动生成getter/setter/toString等
    public static class AgentRequest {
        private String sessionId;
        private String message;
        private Long userId;           // 当前用户ID
        private Boolean isAdmin;       // 是否管理员
        private Map<String, Object> context;
    }
    
    /**
     * Agent响应对象
     * 
     * 【字段说明】
     * - sessionId: 会话ID
     * - success: 是否执行成功
     * - result: 执行结果（自然语言）
     * - error: 错误信息（失败时）
     * - steps: 执行步骤列表
     * - executionTime: 执行耗时（毫秒）
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
     * 
     * 【用途】
     * 记录Agent执行任务的每个步骤，
     * 前端可以展示执行进度
     */
    @Data
    public static class AgentStep {
        private String name;         // 步骤名称
        private String description;  // 步骤描述
        private boolean completed;   // 是否完成
        private String result;       // 步骤结果
        
        public AgentStep(String name, String description, boolean completed) {
            this.name = name;
            this.description = description;
            this.completed = completed;
        }
    }
    
    /**
     * 工具信息
     * 
     * 【用途】
     * 描述Agent可用的工具，供前端展示
     */
    @Data
    public static class ToolInfo {
        private String name;         // 工具名称（代码中的方法名）
        private String displayName;  // 显示名称（给用户看的）
        private String description;  // 工具描述
        
        public ToolInfo(String name, String displayName, String description) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
        }
    }
    
    /**
     * 流式回调接口
     * 
     * 【用途】
     * 定义流式执行时的回调方法，
     * 实现类可以将状态推送给前端（如通过SSE）
     */
    public interface AgentStreamCallback {
        void onStart(String sessionId);           // 开始执行
        void onStep(String step, String description);  // 执行步骤
        void onResult(String result);             // 返回结果
        void onError(String error);               // 发生错误
        void onComplete();                        // 执行完成
    }
}
