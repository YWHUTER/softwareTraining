package com.campus.news.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户关注关系实体
 */
@Data
@TableName("user_follow")
public class UserFollow {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关注者ID（粉丝）
     */
    private Long followerId;
    
    /**
     * 被关注者ID
     */
    private Long followingId;
    
    /**
     * 关注时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
