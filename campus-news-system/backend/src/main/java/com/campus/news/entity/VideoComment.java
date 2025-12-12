package com.campus.news.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("video_comment")
public class VideoComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long videoId;
    private Long userId;
    private String content;
    private Long parentId;
    private Long replyToUserId;
    private Integer likeCount;
    private Integer status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 非数据库字段
    @TableField(exist = false)
    private User user;
    
    @TableField(exist = false)
    private User replyToUser;
    
    @TableField(exist = false)
    private List<VideoComment> replies;
    
    @TableField(exist = false)
    private Boolean isLiked;
}
