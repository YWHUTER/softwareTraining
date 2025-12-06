package com.campus.news.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.news.common.Result;
import com.campus.news.dto.ChatSessionDTO;
import com.campus.news.entity.ChatMessage;
import com.campus.news.entity.ChatSession;
import com.campus.news.entity.User;
import com.campus.news.service.ChatHistoryService;
import com.campus.news.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI聊天历史记录控制器
 */
@Slf4j
@Tag(name = "AI聊天历史接口")
@RestController
@RequestMapping("/ai/history")
@RequiredArgsConstructor
public class ChatHistoryController {
    
    private final ChatHistoryService chatHistoryService;
    private final UserService userService;
    
    /**
     * 获取当前用户的会话列表
     */
    @Operation(summary = "获取会话列表")
    @GetMapping("/sessions")
    public Result<List<ChatSessionDTO>> getSessions(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        List<ChatSession> sessions = chatHistoryService.getUserSessions(userId);
        
        List<ChatSessionDTO> dtos = sessions.stream().map(session -> {
            ChatSessionDTO dto = new ChatSessionDTO();
            dto.setId(session.getId());
            dto.setTitle(session.getTitle());
            dto.setModel(session.getModel());
            dto.setMessageCount(session.getMessageCount());
            dto.setCreatedAt(session.getCreatedAt());
            dto.setUpdatedAt(session.getUpdatedAt());
            return dto;
        }).collect(Collectors.toList());
        
        return Result.success(dtos);
    }
    
    /**
     * 获取会话详情（包含消息）
     */
    @Operation(summary = "获取会话详情")
    @GetMapping("/sessions/{sessionId}")
    public Result<ChatSessionDTO> getSessionDetail(@PathVariable Long sessionId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        
        ChatSession session = chatHistoryService.getSession(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return Result.error("会话不存在");
        }
        
        ChatSessionDTO dto = new ChatSessionDTO();
        dto.setId(session.getId());
        dto.setTitle(session.getTitle());
        dto.setModel(session.getModel());
        dto.setMessageCount(session.getMessageCount());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        dto.setMessages(chatHistoryService.getSessionMessages(sessionId));
        
        return Result.success(dto);
    }
    
    /**
     * 创建新会话
     */
    @Operation(summary = "创建新会话")
    @PostMapping("/sessions")
    public Result<ChatSession> createSession(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        String model = request.getOrDefault("model", "kimi");
        String firstMessage = request.getOrDefault("firstMessage", "新对话");
        
        ChatSession session = chatHistoryService.createSession(userId, model, firstMessage);
        return Result.success(session);
    }
    
    /**
     * 保存消息到会话
     */
    @Operation(summary = "保存消息")
    @PostMapping("/sessions/{sessionId}/messages")
    public Result<Void> saveMessage(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        
        // 验证会话所有权
        ChatSession session = chatHistoryService.getSession(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return Result.error("会话不存在");
        }
        
        String role = request.get("role");
        String content = request.get("content");
        
        if (role == null || content == null) {
            return Result.error("参数不完整");
        }
        
        chatHistoryService.saveMessage(sessionId, role, content);
        return Result.success();
    }
    
    /**
     * 删除会话
     */
    @Operation(summary = "删除会话")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        
        ChatSession session = chatHistoryService.getSession(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return Result.error("会话不存在");
        }
        
        chatHistoryService.deleteSession(sessionId);
        return Result.success();
    }
    
    /**
     * 更新会话标题
     */
    @Operation(summary = "更新会话标题")
    @PutMapping("/sessions/{sessionId}/title")
    public Result<Void> updateTitle(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        
        ChatSession session = chatHistoryService.getSession(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            return Result.error("会话不存在");
        }
        
        String title = request.get("title");
        if (title == null || title.trim().isEmpty()) {
            return Result.error("标题不能为空");
        }
        
        chatHistoryService.updateSessionTitle(sessionId, title.trim());
        return Result.success();
    }
    
    private Long getCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getOne(new QueryWrapper<User>().eq("username", username));
        return user.getId();
    }
}
