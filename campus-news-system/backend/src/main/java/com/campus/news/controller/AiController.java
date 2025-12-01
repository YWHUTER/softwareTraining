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
 * AI 助手控制器
 * 提供 AI 聊天相关接口
 */
@Slf4j
@Tag(name = "AI助手接口")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {
    
    private final AIService aiService;
    
    /**
     * AI 聊天接口
     * POST /api/ai/chat
     * 
     * @param request 聊天请求，包含 question 字段
     * @return AI 回复内容
     */
    @Operation(summary = "AI聊天")
    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        log.info("收到AI聊天请求: {}", request.getQuestion());
        
        try {
            AiChatResponse response = aiService.chat(request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI聊天处理失败", e);
            return Result.error("AI服务暂时不可用，请稍后重试");
        }
    }
    
    /**
     * 获取对话历史（可选接口）
     * GET /api/ai/history/{sessionId}
     */
    @Operation(summary = "获取对话历史")
    @GetMapping("/history/{sessionId}")
    public Result<String> getHistory(@PathVariable String sessionId) {
        String history = aiService.getConversationHistory(sessionId);
        return Result.success(history);
    }
    
    /**
     * 健康检查接口
     * GET /api/ai/health
     */
    @Operation(summary = "AI服务健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("AI服务运行正常");
    }
}
