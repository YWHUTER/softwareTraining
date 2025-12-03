package com.campus.news.service;

import com.campus.news.entity.User;
import com.campus.news.mapper.UserMapper;
import com.campus.news.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 实时通知服务
 * 统一处理 WebSocket 实时消息推送 + 数据库持久化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeNotificationService {
    
    private final NotificationWebSocketHandler webSocketHandler;
    private final UserMapper userMapper;
    @Lazy
    private final NotificationService notificationService;
    
    /**
     * 发送点赞通知
     * 
     * @param targetUserId 被点赞文章的作者ID
     * @param likerUserId 点赞者ID
     * @param articleId 文章ID
     * @param articleTitle 文章标题
     */
    public void sendLikeNotification(Long targetUserId, Long likerUserId, Long articleId, String articleTitle) {
        // 不给自己发通知
        if (targetUserId.equals(likerUserId)) return;
        
        User liker = userMapper.selectById(likerUserId);
        if (liker == null) return;
        
        String likerName = liker.getRealName() != null ? liker.getRealName() : liker.getUsername();
        String content = String.format("%s 赞了你的文章《%s》", likerName, truncate(articleTitle, 20));
        
        // 💾 保存到数据库
        notificationService.createNotification(targetUserId, likerUserId, "LIKE", articleId, null, content);
        
        // 📨 发送实时推送
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "LIKE");
        notification.put("title", "收到新点赞");
        notification.put("content", content);
        notification.put("articleId", articleId);
        notification.put("fromUserId", likerUserId);
        notification.put("fromUserName", likerName);
        notification.put("fromUserAvatar", liker.getAvatar());
        notification.put("timestamp", formatTime());
        
        webSocketHandler.sendNotificationToUser(targetUserId, notification);
    }
    
    /**
     * 发送评论通知
     * 
     * @param targetUserId 被评论文章的作者ID
     * @param commenterUserId 评论者ID
     * @param articleId 文章ID
     * @param articleTitle 文章标题
     * @param commentContent 评论内容
     */
    public void sendCommentNotification(Long targetUserId, Long commenterUserId, Long articleId, 
                                         String articleTitle, String commentContent) {
        // 不给自己发通知
        if (targetUserId.equals(commenterUserId)) return;
        
        User commenter = userMapper.selectById(commenterUserId);
        if (commenter == null) return;
        
        String commenterName = commenter.getRealName() != null ? commenter.getRealName() : commenter.getUsername();
        String content = String.format("%s 评论了你的文章《%s》：%s", 
                commenterName, truncate(articleTitle, 15), truncate(commentContent, 30));
        
        // 💾 保存到数据库
        notificationService.createNotification(targetUserId, commenterUserId, "COMMENT", articleId, null, content);
        
        // 📨 发送实时推送
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "COMMENT");
        notification.put("title", "收到新评论");
        notification.put("content", content);
        notification.put("articleId", articleId);
        notification.put("fromUserId", commenterUserId);
        notification.put("fromUserName", commenterName);
        notification.put("fromUserAvatar", commenter.getAvatar());
        notification.put("timestamp", formatTime());
        
        webSocketHandler.sendNotificationToUser(targetUserId, notification);
    }
    
    /**
     * 发送关注通知
     * 
     * @param targetUserId 被关注者ID
     * @param followerUserId 关注者ID
     */
    public void sendFollowNotification(Long targetUserId, Long followerUserId) {
        User follower = userMapper.selectById(followerUserId);
        if (follower == null) return;
        
        String followerName = follower.getRealName() != null ? follower.getRealName() : follower.getUsername();
        String content = String.format("%s 关注了你", followerName);
        
        // 💾 保存到数据库
        notificationService.createNotification(targetUserId, followerUserId, "FOLLOW", null, null, content);
        
        // 📨 发送实时推送
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "FOLLOW");
        notification.put("title", "新粉丝");
        notification.put("content", content);
        notification.put("fromUserId", followerUserId);
        notification.put("fromUserName", followerName);
        notification.put("fromUserAvatar", follower.getAvatar());
        notification.put("timestamp", formatTime());
        
        webSocketHandler.sendNotificationToUser(targetUserId, notification);
    }
    
    /**
     * 发送收藏通知
     */
    public void sendFavoriteNotification(Long targetUserId, Long userId, Long articleId, String articleTitle) {
        if (targetUserId.equals(userId)) return;
        
        User user = userMapper.selectById(userId);
        if (user == null) return;
        
        String userName = user.getRealName() != null ? user.getRealName() : user.getUsername();
        String content = String.format("%s 收藏了你的文章《%s》", userName, truncate(articleTitle, 20));
        
        // 💾 保存到数据库
        notificationService.createNotification(targetUserId, userId, "FAVORITE", articleId, null, content);
        
        // 📨 发送实时推送
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "FAVORITE");
        notification.put("title", "文章被收藏");
        notification.put("content", content);
        notification.put("articleId", articleId);
        notification.put("fromUserId", userId);
        notification.put("fromUserName", userName);
        notification.put("fromUserAvatar", user.getAvatar());
        notification.put("timestamp", formatTime());
        
        webSocketHandler.sendNotificationToUser(targetUserId, notification);
    }
    
    /**
     * 发送系统通知（广播）
     */
    public void sendSystemNotification(String title, String content) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "SYSTEM");
        notification.put("title", title);
        notification.put("content", content);
        notification.put("timestamp", formatTime());
        
        webSocketHandler.broadcast(notification);
    }
    
    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
    
    /**
     * 格式化当前时间
     */
    private String formatTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
