package com.campus.news.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    private String content;
    private String summary;
    private String coverImage;
    private Long authorId;
    private String boardType; // OFFICIAL, CAMPUS, COLLEGE
    private Long collegeId;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer isPinned;
    private Integer isApproved;
    
    @TableLogic
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableField(exist = false)
    private User author;
    
    @TableField(exist = false)
    private College college;
    
    @TableField(exist = false)
    private Boolean isLiked;
    
    @TableField(exist = false)
    private Boolean isFavorited;
}
