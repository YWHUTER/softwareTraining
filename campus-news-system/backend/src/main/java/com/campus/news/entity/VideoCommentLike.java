package com.campus.news.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_comment_like")
public class VideoCommentLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long commentId;
    private Long userId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
