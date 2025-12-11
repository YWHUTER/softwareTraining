package com.campus.news.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("video_like")
public class VideoLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long videoId;
    private Long userId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
