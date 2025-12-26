package com.campus.news.controller;

import com.campus.news.agent.NewsAgentService;
import com.campus.news.agent.NewsAgentService.*;
import com.campus.news.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ============================================================================
 * AI Agent控制器 (AgentController)
 * ============================================================================
 * 
 * 【核心功能】
 * 提供智能Agent任务执行的REST API接口
 * Agent可以理解自然语言指令，自动调用工具完成任务
 * 
 * 【接口列表】
 * 1. POST /api/ai/agent/execute        - 执行Agent任务（同步）
 * 2. POST /api/ai/agent/execute/stream - 流式执行Agent任务（SSE）
 * 3. GET  /api/ai/agent/tools          - 获取可用工具列表
 * 4. DELETE /api/ai/agent/session/{id} - 清除会话记忆
 * 5. GET  /api/ai/agent/capabilities   - 获取Agent能力介绍
 * 
 * 【什么是SSE？】
 * Server-Sent Events，服务器推送事件
 * 允许服务器主动向客户端推送数据，实现实时更新
 * 
 * 【答辩要点】
 * Q: Agent和普通AI聊天有什么区别？
 * A: Agent可以调用工具执行实际操作（搜索、发布、统计），
 *    普通AI只能对话回答问题
 * 
 * Q: 什么是SSE流式接口？
 * A: 服务器可以持续推送数据给客户端，用于实时显示Agent执行进度
 */
@Slf4j
@Tag(name = "AI Agent接口", description = "智能Agent任务执行相关接口")
@RestController
@RequestMapping("/ai/agent")  // 基础路径：/api/ai/agent
@RequiredArgsConstructor
public class AgentController {
    
    private final NewsAgentService agentService;  // Agent服务
    
    // 线程池：用于异步执行流式任务
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    // ==================== 核心接口 ====================
    
    /**
     * 执行Agent任务（同步方式）
     * 
     * 【接口说明】
     * POST /api/ai/agent/execute
     * 
     * 【请求体格式】
     * {
     *   "message": "帮我搜索关于校园活动的新闻",
     *   "sessionId": "xxx",     // 可选，用于多轮对话
     *   "userId": 1,            // 当前用户ID
     *   "isAdmin": false        // 是否管理员
     * }
     * 
     * 【响应格式】
     * {
     *   "code": 200,
     *   "data": {
     *     "sessionId": "xxx",
     *     "success": true,
     *     "result": "找到5篇相关文章...",
     *     "steps": [...],
     *     "executionTime": 1234
     *   }
     * }
     * 
     * 【执行流程】
     * 1. 接收用户消息
     * 2. Agent分析意图，选择工具
     * 3. 执行工具方法
     * 4. 整理结果返回
     * 
     * @param request Agent请求
     * @return Agent执行结果
     */
    @Operation(summary = "执行Agent任务", description = "让AI Agent自动执行任务，如搜索文章、创建内容、数据统计等")
    @PostMapping("/execute")
    public Result<AgentResponse> executeTask(@Valid @RequestBody AgentRequest request) {
        log.info("收到Agent任务请求: {}", request.getMessage());
        
        try {
            AgentResponse response = agentService.executeTask(request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("Agent任务执行失败", e);
            return Result.error("Agent执行失败：" + e.getMessage());
        }
    }
    
    /**
     * 流式执行Agent任务
     * 
     * 【接口说明】
     * POST /api/ai/agent/execute/stream
     * 
     * 【什么是流式执行？】
     * 使用SSE（Server-Sent Events）技术，
     * 服务器可以持续推送执行状态给客户端，
     * 实现类似ChatGPT的打字机效果
     * 
     * 【SSE事件格式】
     * event: start
     * data: {"sessionId": "xxx"}
     * 
     * event: step
     * data: {"step": "理解意图", "description": "正在分析..."}
     * 
     * event: result
     * data: {"content": "执行结果..."}
     * 
     * event: complete
     * data: {"done": true}
     * 
     * 【前端使用方式】
     * const eventSource = new EventSource('/api/ai/agent/execute/stream');
     * eventSource.onmessage = (event) => { ... };
     * 
     * @param request Agent请求
     * @return SSE发射器
     */
    @Operation(summary = "流式执行Agent任务", description = "实时展示Agent的思考和执行过程")
    @PostMapping(value = "/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeTaskStream(@Valid @RequestBody AgentRequest request) {
        log.info("收到Agent流式任务请求: {}", request.getMessage());
        
        // 创建SSE发射器，设置3分钟超时
        SseEmitter emitter = new SseEmitter(180000L);
        
        // 异步执行任务（不阻塞主线程）
        executorService.execute(() -> {
            try {
                // 调用Agent服务的流式执行方法
                agentService.executeTaskStream(request, new AgentStreamCallback() {
                    @Override
                    public void onStart(String sessionId) {
                        // 发送开始事件
                        sendEvent(emitter, "start", Map.of("sessionId", sessionId));
                    }
                    
                    @Override
                    public void onStep(String step, String description) {
                        // 发送步骤事件
                        sendEvent(emitter, "step", Map.of(
                            "step", step,
                            "description", description
                        ));
                    }
                    
                    @Override
                    public void onResult(String result) {
                        // 发送结果事件
                        sendEvent(emitter, "result", Map.of("content", result));
                    }
                    
                    @Override
                    public void onError(String error) {
                        // 发送错误事件
                        sendEvent(emitter, "error", Map.of("message", error));
                        emitter.complete();
                    }
                    
                    @Override
                    public void onComplete() {
                        // 发送完成事件
                        sendEvent(emitter, "complete", Map.of("done", true));
                        emitter.complete();
                    }
                });
            } catch (Exception e) {
                log.error("流式执行失败", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {}
            }
        });
        
        return emitter;
    }
    
    // ==================== 辅助接口 ====================
    
    /**
     * 获取可用工具列表
     * 
     * 【接口说明】
     * GET /api/ai/agent/tools
     * 
     * 【用途】
     * 前端可以展示Agent能做什么，
     * 帮助用户了解可以让Agent执行哪些任务
     * 
     * @return 工具列表
     */
    @Operation(summary = "获取可用工具", description = "获取Agent可以使用的所有工具列表")
    @GetMapping("/tools")
    public Result<List<ToolInfo>> getAvailableTools() {
        List<ToolInfo> tools = agentService.getAvailableTools();
        return Result.success(tools);
    }
    
    /**
     * 清除会话记忆
     * 
     * 【接口说明】
     * DELETE /api/ai/agent/session/{sessionId}
     * 
     * 【用途】
     * 清除指定会话的对话历史，
     * 相当于"开始新对话"
     * 
     * @param sessionId 会话ID
     * @return 操作结果
     */
    @Operation(summary = "清除会话", description = "清除指定会话的上下文记忆")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> clearSession(@PathVariable String sessionId) {
        agentService.clearSession(sessionId);
        return Result.success();
    }
    
    /**
     * 获取Agent能力介绍
     * 
     * 【接口说明】
     * GET /api/ai/agent/capabilities
     * 
     * 【用途】
     * 返回Agent的详细能力说明和使用示例，
     * 帮助用户快速上手
     * 
     * @return Agent能力说明
     */
    @Operation(summary = "获取Agent能力", description = "获取Agent的功能介绍和使用说明")
    @GetMapping("/capabilities")
    public Result<Map<String, Object>> getCapabilities() {
        Map<String, Object> capabilities = new HashMap<>();
        
        // 基本信息
        capabilities.put("name", "校园新闻智能Agent");
        capabilities.put("version", "1.0.0");
        capabilities.put("description", "基于LangChain4j的智能Agent，可以自动执行各种任务");
        
        // 功能分类
        Map<String, List<String>> features = new HashMap<>();
        features.put("搜索查询", List.of(
            "搜索文章内容",
            "查看热门文章",
            "获取最新发布",
            "浏览分类列表"
        ));
        features.put("数据分析", List.of(
            "系统数据统计",
            "用户排行榜",
            "文章浏览分析",
            "分类统计报告"
        ));
        features.put("内容创作", List.of(
            "创建文章草稿",
            "生成文章摘要",
            "内容推荐建议"
        ));
        features.put("智能交互", List.of(
            "理解自然语言",
            "多步骤任务执行",
            "上下文记忆",
            "智能问答"
        ));
        capabilities.put("features", features);
        
        // 使用示例（帮助用户快速上手）
        List<Map<String, String>> examples = List.of(
            Map.of(
                "input", "帮我搜索关于校园活动的新闻",
                "description", "搜索包含特定关键词的文章"
            ),
            Map.of(
                "input", "给我看看浏览量最高的10篇文章",
                "description", "获取热门文章排行榜"
            ),
            Map.of(
                "input", "统计一下系统现在有多少用户和文章",
                "description", "获取系统统计数据"
            ),
            Map.of(
                "input", "帮我创建一篇关于迎新晚会的文章草稿",
                "description", "自动创建文章草稿"
            ),
            Map.of(
                "input", "谁是粉丝最多的用户？",
                "description", "查询用户排行榜"
            )
        );
        capabilities.put("examples", examples);
        
        return Result.success(capabilities);
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 发送SSE事件
     * 
     * 【SSE事件格式】
     * event: {eventName}
     * data: {JSON数据}
     * 
     * @param emitter   SSE发射器
     * @param eventName 事件名称
     * @param data      事件数据
     */
    private void sendEvent(SseEmitter emitter, String eventName, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)  // 事件名称
                    .data(data, MediaType.APPLICATION_JSON));  // JSON格式数据
        } catch (IOException e) {
            log.error("发送SSE事件失败", e);
        }
    }
}
