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
    private Boolean showAll = false; // 管理后台使用，显示所有状态的视频
    private String sortBy; // views, date
    private String sortOrder; // asc, desc
}
