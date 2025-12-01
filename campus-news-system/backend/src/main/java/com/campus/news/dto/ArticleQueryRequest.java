package com.campus.news.dto;

import lombok.Data;

@Data
public class ArticleQueryRequest {
    private Long current = 1L;
    private Long size = 10L;
    private String boardType;
    private Long collegeId;
    private Long authorId;
    private String keyword;
    private Integer isApproved;
    private Integer isPinned;
}
