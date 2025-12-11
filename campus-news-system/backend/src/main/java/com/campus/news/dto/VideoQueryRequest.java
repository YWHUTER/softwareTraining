package com.campus.news.dto;

import lombok.Data;

@Data
public class VideoQueryRequest {
    private Integer current = 1;
    private Integer size = 12;
    private Long categoryId;
    private String categoryCode;
    private Long authorId;
    private String keyword;
    private Integer isApproved;
    private String sortBy; // views, date
    private String sortOrder; // asc, desc
}
