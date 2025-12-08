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
 * AI Agent控制器
 * 提供智能Agent任务执行接口
 */
@Slf4j
@Tag(name = "AI Agent接口", description = "智能Agent任务执行相关接口")
@RestController
@RequestMapping("/ai/agent")
@RequiredArgsConstructor
public class AgentController {
    
    private final NewsAgentService agentService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * 执行Agent任务
     * POST /api/ai/agent/execute
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
     * POST /api/ai/agent/execute/stream
     * 
     * 返回SSE流，实时展示Agent执行过程
     */
    @Operation(summary = "流式执行Agent任务", description = "实时展示Agent的思考和执行过程")
    @PostMapping(value = "/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeTaskStream(@Valid @RequestBody AgentRequest request) {
        log.info("收到Agent流式任务请求: {}", request.getMessage());
        
        // 创建SSE发射器，设置3分钟超时
        SseEmitter emitter = new SseEmitter(180000L);
        
        // 异步执行任务
        executorService.execute(() -> {
            try {
                agentService.executeTaskStream(request, new AgentStreamCallback() {
                    @Override
                    public void onStart(String sessionId) {
                        sendEvent(emitter, "start", Map.of("sessionId", sessionId));
                    }
                    
                    @Override
                    public void onStep(String step, String description) {
                        sendEvent(emitter, "step", Map.of(
                            "step", step,
                            "description", description
                        ));
                    }
                    
                    @Override
                    public void onResult(String result) {
                        sendEvent(emitter, "result", Map.of("content", result));
                    }
                    
                    @Override
                    public void onError(String error) {
                        sendEvent(emitter, "error", Map.of("message", error));
                        emitter.complete();
                    }
                    
                    @Override
                    public void onComplete() {
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
    
    /**
     * 获取可用工具列表
     * GET /api/ai/agent/tools
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
     * DELETE /api/ai/agent/session/{sessionId}
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
     * GET /api/ai/agent/capabilities
     * 
     * @return Agent能力说明
     */
    @Operation(summary = "获取Agent能力", description = "获取Agent的功能介绍和使用说明")
    @GetMapping("/capabilities")
    public Result<Map<String, Object>> getCapabilities() {
        Map<String, Object> capabilities = new HashMap<>();
        
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
        
        // 使用示例
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
    
    /**
     * 发送SSE事件
     */
    private void sendEvent(SseEmitter emitter, String eventName, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.error("发送SSE事件失败", e);
        }
    }
}
