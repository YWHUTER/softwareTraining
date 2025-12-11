package com.campus.news.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video")
public class Video {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    private String description;
    private String videoUrl;
    private String thumbnail;
    private String duration;
    private Integer durationSeconds;
    private Long fileSize;
    private Long categoryId;
    private Long authorId;
    private String channelName;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer isApproved;
    
    @TableLogic(value = "1", delval = "0")
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    // 非数据库字段
    @TableField(exist = false)
    private User author;
    
    @TableField(exist = false)
    private VideoCategory category;
    
    @TableField(exist = false)
    private Boolean isLiked;
}
