package com.campus.news.controller;

import com.campus.news.ai.AIService;
import com.campus.news.dto.AiChatRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 流式输出控制器
 * 使用 Server-Sent Events (SSE) 实现类ChatGPT的打字机效果
 */
@Slf4j
@Tag(name = "AI流式接口")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiStreamController {
    
    private final AIService aiService;
    
    /**
     * AI 流式聊天接口
     * POST /api/ai/chat/stream
     * 
     * 返回 SSE 流，实现打字机效果
     */
    @Operation(summary = "AI流式聊天")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody AiChatRequest request) {
        log.info("收到AI流式聊天请求: {}", request.getQuestion());
        
        // 创建 SSE 发射器，超时时间 3 分钟
        SseEmitter emitter = new SseEmitter(180000L);
        
        // 异步处理流式请求
        aiService.streamChat(request, emitter);
        
        return emitter;
    }
}
