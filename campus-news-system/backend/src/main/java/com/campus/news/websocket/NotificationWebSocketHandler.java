package com.campus.news.websocket;

import com.campus.news.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 通知处理器
 * 管理用户连接，实现实时消息推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 存储用户ID与WebSocket会话的映射
    private static final Map<Long, WebSocketSession> USER_SESSIONS = new ConcurrentHashMap<>();
    
    /**
     * 连接建立后调用
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("📡 WebSocket连接请求到达, URI: {}", session.getUri());
        
        // 从URL参数获取token进行认证
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        log.info("📡 Query参数: {}", query);
        
        String token = extractToken(query);
        log.info("📡 提取的Token: {}", token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null");
        
        if (token != null && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            USER_SESSIONS.put(userId, session);
            log.info("🔗 WebSocket连接建立 - 用户ID: {}, 当前在线: {}人", userId, USER_SESSIONS.size());
            
            // 发送连接成功消息
            sendMessage(session, Map.of(
                "type", "CONNECTED",
                "message", "连接成功",
                "onlineCount", USER_SESSIONS.size()
            ));
        } else {
            log.warn("❌ WebSocket认证失败，关闭连接, token验证结果: {}", token != null ? jwtUtil.validateToken(token) : "token为空");
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }
    
    /**
     * 连接关闭后调用
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 移除会话
        USER_SESSIONS.entrySet().removeIf(entry -> entry.getValue().equals(session));
        log.info("🔌 WebSocket连接关闭 - 当前在线: {}人", USER_SESSIONS.size());
    }
    
    /**
     * 收到消息时调用（心跳检测）
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("ping".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }
    
    /**
     * 发生错误时调用
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误", exception);
        USER_SESSIONS.entrySet().removeIf(entry -> entry.getValue().equals(session));
    }
    
    /**
     * 向指定用户发送通知
     * 
     * @param userId 目标用户ID
     * @param notification 通知内容
     */
    public void sendNotificationToUser(Long userId, Map<String, Object> notification) {
        WebSocketSession session = USER_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                sendMessage(session, notification);
                log.info("📨 实时通知已发送 - 用户ID: {}, 类型: {}", userId, notification.get("type"));
            } catch (Exception e) {
                log.error("发送通知失败 - 用户ID: {}", userId, e);
            }
        }
    }
    
    /**
     * 广播消息给所有在线用户
     */
    public void broadcast(Map<String, Object> message) {
        USER_SESSIONS.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    sendMessage(session, message);
                } catch (Exception e) {
                    log.error("广播消息失败", e);
                }
            }
        });
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = USER_SESSIONS.get(userId);
        return session != null && session.isOpen();
    }
    
    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return USER_SESSIONS.size();
    }
    
    /**
     * 发送JSON消息
     */
    private void sendMessage(WebSocketSession session, Map<String, Object> data) throws IOException {
        String json = objectMapper.writeValueAsString(data);
        session.sendMessage(new TextMessage(json));
    }
    
    /**
     * 从查询字符串提取token
     */
    private String extractToken(String query) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && "token".equals(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }
}
