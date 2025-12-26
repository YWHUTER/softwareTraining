package com.campus.news.controller;

import com.campus.news.ai.AIService;
import com.campus.news.common.Result;
import com.campus.news.dto.AiChatRequest;
import com.campus.news.dto.AiChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================================
 * AI 助手控制器 (AiController)
 * ============================================================================
 * 
 * 【核心功能】
 * 提供AI聊天对话的REST API接口
 * 这是前端AI助手组件调用的后端接口
 * 
 * 【接口列表】
 * 1. POST /api/ai/chat     - AI聊天接口（核心）
 * 2. GET  /api/ai/history  - 获取对话历史
 * 3. GET  /api/ai/health   - 健康检查
 * 
 * 【与Agent的区别】
 * - AiController：普通AI聊天，只能对话，不能执行操作
 * - AgentController：智能Agent，可以调用工具执行任务
 * 
 * 【答辩要点】
 * Q: AI聊天和Agent有什么区别？
 * A: AI聊天只能回答问题，Agent可以执行操作（如搜索、发布文章）
 */
@Slf4j
@Tag(name = "AI助手接口")  // Swagger文档标签
@RestController
@RequestMapping("/ai")  // 基础路径：/api/ai
@RequiredArgsConstructor
public class AiController {
    
    private final AIService aiService;  // AI服务
    
    /**
     * AI 聊天接口
     * 
     * 【接口说明】
     * POST /api/ai/chat
     * 
     * 【请求体格式】
     * {
     *   "question": "你好，请介绍一下这个系统",
     *   "sessionId": "xxx",  // 可选，用于多轮对话
     *   "model": "kimi"      // 可选，选择AI模型
     * }
     * 
     * 【响应格式】
     * {
     *   "code": 200,
     *   "data": {
     *     "answer": "你好！我是校园新闻助手...",
     *     "sessionId": "xxx",
     *     "timestamp": 1234567890
     *   }
     * }
     * 
     * @param request 聊天请求
     * @return AI回复
     */
    @Operation(summary = "AI聊天")
    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        log.info("收到AI聊天请求: {}", request.getQuestion());
        
        try {
            // 调用AI服务处理聊天请求
            AiChatResponse response = aiService.chat(request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI聊天处理失败", e);
            return Result.error("AI服务暂时不可用，请稍后重试");
        }
    }
    
    /**
     * 获取对话历史
     * 
     * 【接口说明】
     * GET /api/ai/history/{sessionId}
     * 
     * 【用途】
     * 获取指定会话的历史对话记录，
     * 用于恢复之前的对话上下文
     * 
     * @param sessionId 会话ID
     * @return 对话历史
     */
    @Operation(summary = "获取对话历史")
    @GetMapping("/history/{sessionId}")
    public Result<String> getHistory(@PathVariable String sessionId) {
        String history = aiService.getConversationHistory(sessionId);
        return Result.success(history);
    }
    
    /**
     * 健康检查接口
     * 
     * 【接口说明】
     * GET /api/ai/health
     * 
     * 【用途】
     * 检查AI服务是否正常运行，
     * 前端可以根据这个接口决定是否显示AI功能
     */
    @Operation(summary = "AI服务健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("AI服务运行正常");
    }
}
