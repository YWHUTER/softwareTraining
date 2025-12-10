package com.campus.news.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long articleId;
    private Long userId;
    private String content;
    private Long parentId;
    
    /**
     * 回复目标用户ID（用于多级回复显示"回复 @xxx"）
     */
    private Long replyToUserId;
    
    /**
     * 根评论ID（所有回复都归属于哪个顶级评论）
     */
    private Long rootId;
    
    private Integer likeCount;
    
    @TableLogic(value = "1", delval = "0")
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableField(exist = false)
    private User user;
    
    @TableField(exist = false)
    private User replyToUser;
    
    @TableField(exist = false)
    private List<Comment> replies;
    
    @TableField(exist = false)
    private Article article;
}
