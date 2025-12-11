package com.campus.news.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VideoCreateRequest {
    @NotBlank(message = "视频标题不能为空")
    private String title;
    
    private String description;
    private String videoUrl;
    private String thumbnail;
    private String duration;
    private Integer durationSeconds;
    private Long fileSize;
    private Long categoryId;
    private String channelName;
}
