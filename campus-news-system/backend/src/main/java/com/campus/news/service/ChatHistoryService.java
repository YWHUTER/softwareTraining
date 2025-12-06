package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.news.entity.ChatMessage;
import com.campus.news.entity.ChatSession;
import com.campus.news.mapper.ChatMessageMapper;
import com.campus.news.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI聊天历史记录服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {
    
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    
    /**
     * 创建新会话
     */
    @Transactional
    public ChatSession createSession(Long userId, String model, String firstMessage) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setModel(model);
        // 从第一条消息生成标题（取前20个字符）
        String title = firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;
        session.setTitle(title);
        session.setMessageCount(0);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.insert(session);
        log.info("创建新会话: sessionId={}, userId={}", session.getId(), userId);
        return session;
    }
    
    /**
     * 保存消息到会话
     */
    @Transactional
    public void saveMessage(Long sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(message);
        
        // 更新会话的消息数量和更新时间
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setMessageCount(session.getMessageCount() + 1);
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }
    }
    
    /**
     * 获取用户的会话列表
     */
    public List<ChatSession> getUserSessions(Long userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
               .orderByDesc(ChatSession::getUpdatedAt);
        return chatSessionMapper.selectList(wrapper);
    }
    
    /**
     * 获取会话的所有消息
     */
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
               .orderByAsc(ChatMessage::getCreatedAt);
        return chatMessageMapper.selectList(wrapper);
    }
    
    /**
     * 获取会话详情
     */
    public ChatSession getSession(Long sessionId) {
        return chatSessionMapper.selectById(sessionId);
    }
    
    /**
     * 删除会话及其消息
     */
    @Transactional
    public void deleteSession(Long sessionId) {
        // 删除会话下的所有消息
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        chatMessageMapper.delete(wrapper);
        
        // 删除会话
        chatSessionMapper.deleteById(sessionId);
        log.info("删除会话: sessionId={}", sessionId);
    }
    
    /**
     * 更新会话标题
     */
    public void updateSessionTitle(Long sessionId, String title) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setTitle(title);
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionMapper.updateById(session);
        }
    }
}
