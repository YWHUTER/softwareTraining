package com.campus.news.dto;

import com.campus.news.entity.ChatMessage;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天会话DTO
 */
@Data
public class ChatSessionDTO {
    private Long id;
    private String title;
    private String model;
    private Integer messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 会话消息列表（加载详情时使用）
     */
    private List<ChatMessage> messages;
}
