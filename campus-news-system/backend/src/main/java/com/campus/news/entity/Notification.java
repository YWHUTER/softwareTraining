package com.campus.news.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 接收通知的用户ID
     */
    private Long userId;
    
    /**
     * 触发通知的用户ID
     */
    private Long fromUserId;
    
    /**
     * 通知类型: MENTION-被@提及, COMMENT-评论, LIKE-点赞, FOLLOW-关注
     */
    private String type;
    
    /**
     * 相关文章ID（如果有）
     */
    private Long articleId;
    
    /**
     * 相关评论ID（如果有）
     */
    private Long commentId;
    
    /**
     * 通知内容
     */
    private String content;
    
    /**
     * 是否已读: 0-未读 1-已读
     */
    private Integer isRead;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 关联对象（非数据库字段）
    @TableField(exist = false)
    private User fromUser;
    
    @TableField(exist = false)
    private Article article;
}
