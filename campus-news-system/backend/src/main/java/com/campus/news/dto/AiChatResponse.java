package com.campus.news.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 聊天响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    
    /**
     * AI 回复内容
     */
    private String answer;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 响应时间戳
     */
    private Long timestamp;
}
