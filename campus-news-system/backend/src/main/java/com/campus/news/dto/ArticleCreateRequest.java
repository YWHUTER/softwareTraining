package com.campus.news.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleCreateRequest {
    @NotBlank(message = "标题不能为空")
    private String title;
    
    @NotBlank(message = "内容不能为空")
    private String content;
    
    private String summary;
    private String coverImage;
    
    @NotBlank(message = "板块类型不能为空")
    private String boardType; // OFFICIAL, CAMPUS, COLLEGE
    
    private Long collegeId;
    private Integer isPinned = 0;
}
