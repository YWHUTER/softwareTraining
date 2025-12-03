package com.campus.news.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.news.entity.Article;
import com.campus.news.entity.Notification;
import com.campus.news.entity.User;
import com.campus.news.mapper.ArticleMapper;
import com.campus.news.mapper.NotificationMapper;
import com.campus.news.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    
    /**
     * 创建通知
     */
    public void createNotification(Long userId, Long fromUserId, String type, 
                                   Long articleId, Long commentId, String content) {
        // 不给自己发通知
        if (userId.equals(fromUserId)) {
            return;
        }
        
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setFromUserId(fromUserId);
        notification.setType(type);
        notification.setArticleId(articleId);
        notification.setCommentId(commentId);
        notification.setContent(content);
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        
        notificationMapper.insert(notification);
    }
    
    /**
     * 解析评论中的 @ 提及并创建通知
     */
    public void parseAndNotifyMentions(String commentContent, Long fromUserId, 
                                        Long articleId, Long commentId) {
        // 匹配 @用户名 格式
        Pattern pattern = Pattern.compile("@([^\\s@]+)");
        Matcher matcher = pattern.matcher(commentContent);
        
        while (matcher.find()) {
            String mentionedName = matcher.group(1);
            
            // 查找被提及的用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getRealName, mentionedName);
            User mentionedUser = userMapper.selectOne(wrapper);
            
            if (mentionedUser != null) {
                // 获取文章信息
                Article article = articleMapper.selectById(articleId);
                String articleTitle = article != null ? article.getTitle() : "某篇文章";
                
                // 获取发送者信息
                User fromUser = userMapper.selectById(fromUserId);
                String fromName = fromUser != null ? fromUser.getRealName() : "某用户";
                
                String content = fromName + " 在文章「" + articleTitle + "」中@了你";
                
                createNotification(mentionedUser.getId(), fromUserId, "MENTION", 
                                 articleId, commentId, content);
            }
        }
    }
    
    /**
     * 获取用户的通知列表
     */
    public Page<Notification> getNotifications(Long userId, Integer current, Integer size) {
        Page<Notification> page = new Page<>(current, size);
        
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .orderByDesc(Notification::getCreatedAt);
        
        Page<Notification> result = notificationMapper.selectPage(page, wrapper);
        
        // 填充关联数据
        for (Notification notification : result.getRecords()) {
            if (notification.getFromUserId() != null) {
                notification.setFromUser(userMapper.selectById(notification.getFromUserId()));
            }
            if (notification.getArticleId() != null) {
                notification.setArticle(articleMapper.selectById(notification.getArticleId()));
            }
        }
        
        return result;
    }
    
    /**
     * 获取未读通知数量
     */
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }
    
    /**
     * 标记通知为已读
     */
    public void markAsRead(Long notificationId, Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
               .eq(Notification::getUserId, userId)
               .set(Notification::getIsRead, 1);
        notificationMapper.update(null, wrapper);
    }
    
    /**
     * 标记所有通知为已读
     */
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, 0)
               .set(Notification::getIsRead, 1);
        notificationMapper.update(null, wrapper);
    }
}
