package com.campus.news.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 聊天请求 DTO
 */
@Data
public class AiChatRequest {
    
    @NotBlank(message = "问题不能为空")
    private String question;
    
    /**
     * 会话ID（可选，用于上下文管理）
     */
    private String sessionId;
}
